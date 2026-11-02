package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.config.AiProperties;
import com.example.academic_risk_warning.vo.ai.ReportFact;
import com.example.academic_risk_warning.vo.ai.StudentReportVO;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 学生端个人学情报告：**规则算事实 → 模型写人话 → 数字回验 → 不合格退模板**。
 *
 * <p>报告落 {@code ai_report}，每次生成都记 {@code ai_run}，模型不可用不影响出报告。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentReportService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FactPackBuilder factPackBuilder;
    private final ReportComposer composer;
    private final RetrievalService retrievalService;
    private final DifyClient difyClient;
    private final DashScopeClient dashScopeClient;
    private final AiRunService runService;
    private final AiProperties props;
    private final JdbcTemplate jdbc;

    private static final String LIST_SQL = """
            select r.id, r.enrollment_id, r.teaching_class_id, r.offering_id, r.course_id, r.term_id,
                   r.report_type, r.scope, r.risk_level, r.total_score, r.summary,
                   r.content_json, r.source, r.read_time, r.create_time,
                   c.course_name, tc.class_name, t.term_name, te.teacher_name
            from ai_report r
                     join course c on c.id = r.course_id
                     join teaching_class tc on tc.id = r.teaching_class_id
                     join term t on t.id = r.term_id
                     join teacher te on te.id = tc.teacher_id
            """;

    // ================================================================ 生成
    public StudentReportVO generate(Long studentId, Long enrollmentId, String reportType) {
        String type = (reportType == null || reportType.isBlank()) ? "STAGE" : reportType.toUpperCase();
        ReportFact fact = factPackBuilder.build(studentId, enrollmentId);
        // R1：检索本课程资料，报告末尾附「参考依据」，让学生知道依据哪份资料的哪一段
        List<RetrievalService.Hit> evidence = retrieveEvidence(fact);
        String evidenceBlock = evidenceBlock(evidence);

        Long runId = runService.start("REPORT", "ENROLLMENT", enrollmentId, studentId);
        long t0 = System.currentTimeMillis();
        ReportComposer.Content content = null;
        String source = "TEMPLATE";
        String status = "FALLBACK";
        StringBuilder note = new StringBuilder();
        int tokens = 0;

        // 通道一：Dify 工作流（配了 App Key 才走）
        if (props.isEnabled() && props.reportReady()) {
            if (runService.overLimit(studentId, "REPORT")) {
                note.append("已达当日调用上限；");
            } else {
                Map<String, Object> inputs = composer.promptInputs(fact);
                if (!evidenceBlock.isEmpty()) {
                    inputs.put("facts", String.valueOf(inputs.get("facts")) + evidenceBlock);
                }
                DifyClient.WorkflowResult res = difyClient.runWorkflow(props.getReportAppKey(), inputs, "student-" + studentId);
                tokens += res.getTotalTokens();
                if (!res.isOk()) {
                    note.append("Dify 失败：").append(res.getError()).append("；");
                    runService.step(runId, 1, "dify-workflow", "report-student", null, "FAIL", res.getError(),
                            System.currentTimeMillis() - t0);
                } else {
                    String text = pickText(res.getOutputs());
                    List<String> bad = composer.invalidNumbersForReport(text, fact);
                    runService.step(runId, 1, "dify-workflow", "report-student", text,
                            bad.isEmpty() ? "PASS" : "WARN",
                            bad.isEmpty() ? null : "越界数字 " + bad.stream().distinct().limit(6).toList(),
                            System.currentTimeMillis() - t0);

                    // Q3：数字回验不做"一票否决"，改成定点修复——把越界的**具体数字**回给模型，
                    // 要求只改这些地方，其余保持不变。整篇重写既浪费 token 又容易把写好的段落改坏。
                    if (!bad.isEmpty() && bad.size() <= 6) {
                        long tRetry = System.currentTimeMillis();
                        Map<String, Object> retryInputs = new java.util.LinkedHashMap<>(inputs);
                        Object facts = retryInputs.get("facts");
                        List<String> distinctBad = bad.stream().distinct().limit(6).toList();
                        retryInputs.put("facts", (facts == null ? "" : String.valueOf(facts))
                                + "\n\n【上一版出现的事实之外的数字】" + String.join("、", distinctBad)
                                + "\n【修复要求】只改这几处：把上述数字删除，或替换成上面已经出现过的数字；"
                                + "其余段落、结构、措辞尽量保持不变。上面没有出现过的数字一律不许再出现，"
                                + "禁止自行计算或换算；确实要表达训练时长/题量时，用文字表述（如「每天若干道」）。");
                        DifyClient.WorkflowResult res2 = difyClient.runWorkflow(props.getReportAppKey(), retryInputs,
                                "student-" + studentId + "-retry");
                        tokens += res2.getTotalTokens();
                        if (res2.isOk()) {
                            String text2 = pickText(res2.getOutputs());
                            List<String> bad2 = composer.invalidNumbersForReport(text2, fact);
                            runService.step(runId, 2, "dify-workflow-repair", "report-student", text2,
                                    bad2.isEmpty() ? "PASS" : "WARN",
                                    bad2.isEmpty() ? "定点修复成功，已消除 " + distinctBad
                                            : "修复后仍越界 " + bad2.stream().distinct().limit(4).toList(),
                                    System.currentTimeMillis() - tRetry);
                            if (bad2.isEmpty() && composer.looksLikeReport(text2)) {
                                text = text2;
                                bad = bad2;
                            }
                        }
                    }

                    if (bad.isEmpty() && composer.looksLikeReport(text)) {
                        content = composer.composeByDify(fact, text);
                        source = "DIFY";
                        status = "SUCCESS";
                    } else if (!bad.isEmpty()) {
                        note.append("Dify 数字回验未通过（已重试一次）；");
                    } else {
                        note.append("Dify 输出不含四段标题（可能全被推理块吞掉）；");
                    }
                }
            }
        } else if (!props.isEnabled()) {
            note.append("AI 已关闭（ai.enabled=false）；");
        } else {
            note.append("未配置 Dify 报告工作流 App Key；");
        }

        // 通道二：DashScope 直连（Dify 不可用或未配时兜底）
        if (content == null && props.directReady() && !runService.overLimit(studentId, "REPORT")) {
            DashScopeClient.ChatResult res = dashScopeClient.chat(composer.promptSystem(), composer.promptUser(fact) + evidenceBlock);
            long t1 = System.currentTimeMillis();
            tokens += res.getTotalTokens();
            if (!res.isOk()) {
                note.append("直连模型失败：").append(res.getError()).append("；");
                runService.step(runId, 2, "dashscope-chat", "report-student", null, "FAIL", res.getError(),
                        System.currentTimeMillis() - t1);
            } else {
                String text = res.getText();
                List<String> bad = composer.invalidNumbersForReport(text, fact);
                runService.step(runId, 2, "dashscope-chat", "report-student", text,
                        bad.isEmpty() ? "PASS" : "WARN",
                        bad.isEmpty() ? null : "越界数字 " + bad.stream().distinct().limit(6).toList(),
                        System.currentTimeMillis() - t1);
                if (!bad.isEmpty() && bad.size() <= 6) {
                    // 带批评重写一次（Self-Refine）
                    long t2 = System.currentTimeMillis();
                    String corrective = composer.promptUser(fact)
                            + "\n\n上一版出现了事实之外的数字：" + String.join("、", bad.stream().distinct().toList())
                            + "。请只使用给定事实中的数字重写；确实需要给出训练时长/题量时，用文字表述（如「每天若干道」）。";
                    DashScopeClient.ChatResult retry = dashScopeClient.chat(composer.promptSystem(), corrective + evidenceBlock);
                    tokens += retry.getTotalTokens();
                    if (retry.isOk()) {
                        List<String> bad2 = composer.invalidNumbersForReport(retry.getText(), fact);
                        runService.step(runId, 3, "dashscope-chat-retry", "report-student", retry.getText(),
                                bad2.isEmpty() ? "PASS" : "FAIL",
                                bad2.isEmpty() ? null : "重写后仍越界 " + bad2.stream().distinct().limit(6).toList(),
                                System.currentTimeMillis() - t2);
                        if (bad2.isEmpty()) {
                            text = retry.getText();
                            bad = List.of();
                        }
                    }
                }
                if (bad.isEmpty() && composer.looksLikeReport(text)) {
                    content = composer.composeByDify(fact, text);
                    source = "LLM";
                    status = "SUCCESS";
                } else if (!bad.isEmpty()) {
                    note.append("直连模型数字回验未通过（已重写一次）；");
                } else {
                    note.append("直连模型输出不含四段标题；");
                }
            }
        }

        // 通道三：规则模板（永远不会失败）
        if (content == null) {
            content = composer.composeByTemplate(fact);
            if (status.equals("FALLBACK")) {
                note.append("已使用规则模板");
            }
        } else {
            note.append("ok");
        }

        // R1：不论走哪条通道，都在末尾补「参考依据」段落（检索为空时如实说明）
        appendEvidence(content, evidence);

        String error = status.equals("SUCCESS") ? null : note.toString();
        // 即使成功，也把"主通道为什么没用上"留痕：报告配了 Dify 却走了直连时，ai_run 里要能看出原因
        String channelNote = null;
        if (status.equals("SUCCESS") && note.length() > 0) {
            String n = note.toString().replaceAll("；?ok$", "");
            if (!n.isBlank()) {
                channelNote = "主通道降级说明：" + n;
            }
        }
        Long reportId = persist(fact, type, content, source, runId, tokens);
        runService.usage(runId, "REPORT", tokens, studentId);
        runService.finish(runId, status, source, tokens, error != null ? error : channelNote);
        return detail(studentId, reportId);
    }

    /** 从工作流输出里取正文：优先 text / result / output，取第一个非空 */
    private String pickText(Map<String, Object> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return null;
        }
        for (String key : List.of("text", "result", "output", "report", "answer")) {
            Object v = outputs.get(key);
            if (v != null && !String.valueOf(v).isBlank()) {
                return String.valueOf(v);
            }
        }
        return outputs.values().stream().filter(Objects::nonNull)
                .map(String::valueOf).filter(s -> s.length() > 40).findFirst().orElse(null);
    }

    private Long persist(ReportFact fact, String type, ReportComposer.Content content,
                         String source, Long runId, int tokens) {
        KeyHolder kh = new GeneratedKeyHolder();
        String factJson = toJson(fact);
        String contentJson = composer.toJson(content);
        BigDecimal cost = BigDecimal.valueOf(props.costOf(tokens));
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    insert into ai_report(student_id, enrollment_id, teaching_class_id, offering_id, course_id, term_id,
                                          report_type, scope, risk_level, total_score, fact_json, content_json, summary,
                                          status, source, model, app_id, prompt_version, run_id, tokens, cost, publish_time)
                    values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setLong(i++, fact.getStudentId());
            ps.setLong(i++, fact.getEnrollmentId());
            ps.setLong(i++, fact.getTeachingClassId());
            ps.setLong(i++, fact.getOfferingId());
            ps.setLong(i++, fact.getCourseId());
            ps.setLong(i++, fact.getTermId());
            ps.setString(i++, type);
            ps.setString(i++, "STUDENT");
            ps.setString(i++, fact.getRiskLevel());
            if (fact.getTotalScore() == null) {
                ps.setNull(i++, java.sql.Types.DECIMAL);
            } else {
                ps.setBigDecimal(i++, fact.getTotalScore());
            }
            ps.setString(i++, factJson);
            ps.setString(i++, contentJson);
            ps.setString(i++, content.getSummary());
            ps.setString(i++, "PUBLISHED");
            ps.setString(i++, source);
            ps.setString(i++, runService.modelLabel(source));
            ps.setString(i++, props.getReportAppKey() == null ? null : maskKey(props.getReportAppKey()));
            ps.setString(i++, props.getReportPromptVersion());
            if (runId == null) {
                ps.setNull(i++, java.sql.Types.BIGINT);
            } else {
                ps.setLong(i++, runId);
            }
            ps.setInt(i++, tokens);
            ps.setBigDecimal(i++, cost);
            ps.setObject(i, LocalDateTime.now());
            return ps;
        }, kh);
        return kh.getKey() == null ? null : kh.getKey().longValue();
    }

    /** 按教学班生成（前端只知道 teachingClassId 时用；解析出本人在该班的选课记录） */
    public StudentReportVO generateByClass(Long studentId, Long teachingClassId, String reportType) {
        List<Long> ids = jdbc.queryForList(
                "select e.id from enrollment e where e.student_id = ? and e.teaching_class_id = ? and e.status in ('ACTIVE','ENROLLED') "
                        + "order by e.id desc limit 1", Long.class, studentId, teachingClassId);
        if (ids.isEmpty()) {
            throw new BizException("你不在该教学班，或选课已失效");
        }
        return generate(studentId, ids.get(0), reportType);
    }

    // ================================================================ R1：课程资料检索
    /**
     * 为这份报告检索支撑资料。
     *
     * <p>查询串取「课程名 + 成绩单状态 + 分项里最弱的两项」：报告要讲的"需要补的地方"往往正对应
     * 某份课件/习题集里的知识点，而不是随便一篇文档。</p>
     */
    private List<RetrievalService.Hit> retrieveEvidence(ReportFact fact) {
        if (fact.getCourseId() == null) {
            return List.of();
        }
        StringBuilder q = new StringBuilder();
        q.append(fact.getCourseName() == null ? "" : fact.getCourseName()).append(' ');
        q.append("期末未计入".equals(fact.getScoreStatus()) || "PARTIAL".equals(fact.getScoreStatus())
                ? "过程分 成绩构成 " : "考核方式 成绩构成 ");
        List<ReportFact.Item> items = fact.getItems() == null ? List.of() : fact.getItems();
        items.stream()
                .filter(i -> i.getScore() != null && !Boolean.TRUE.equals(i.getMissing()))
                .sorted(java.util.Comparator.comparing(ReportFact.Item::getScore))
                .limit(2)
                .forEach(i -> q.append(i.getName()).append(' '));
        try {
            RetrievalService.Outcome out = retrievalService.retrieve(fact.getCourseId(), q.toString().trim(), 3);
            RetrievalService.Guard guard = retrievalService.guard(out, q.toString());
            if (guard.refuse()) {
                log.debug("[Report] 资料检索无足够依据：{}", guard.reason());
                return List.of();
            }
            return out.hits();
        } catch (Exception e) {
            log.warn("[Report] 资料检索失败，报告照常生成：{}", e.getMessage());
            return List.of();
        }
    }

    /** 给模型看的依据块：要求把文档名与段号写进正文末尾，但不得编造资料名 */
    private String evidenceBlock(List<RetrievalService.Hit> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n\n【可引用的课程资料】（原文照抄文档名与段号，不得编造）\n");
        int i = 1;
        for (RetrievalService.Hit h : evidence) {
            String t = h.content() == null ? "" : h.content().replace('\n', ' ').replaceAll("\\s{2,}", " ").trim();
            if (t.length() > 120) {
                t = t.substring(0, 120) + "…";
            }
            sb.append(i++).append("）").append(h.docName()).append(" 第").append(h.sectionNo()).append("段：").append(t).append("\n");
        }
        return sb.toString();
    }

    /** 往报告末尾追加「参考依据」段落；检索为空时如实说明，不假装有依据 */
    private void appendEvidence(ReportComposer.Content content, List<RetrievalService.Hit> evidence) {
        if (content == null) {
            return;
        }
        String text;
        if (evidence == null || evidence.isEmpty()) {
            text = "本课程的课程资料库里暂未检索到与本次评价直接对应的条款。";
        } else {
            StringBuilder sb = new StringBuilder();
            for (RetrievalService.Hit h : evidence) {
                if (sb.length() > 0) {
                    sb.append("；");
                }
                sb.append(h.docName()).append(" 第").append(h.sectionNo()).append("段");
            }
            text = "课程资料：" + sb;
        }
        List<ReportComposer.Section> sections = content.getSections();
        if (sections == null) {
            sections = new ArrayList<>();
            content.setSections(sections);
        }
        sections.add(new ReportComposer.Section("参考依据", text));
    }

    // ================================================================ 查询
    public List<StudentReportVO> list(Long studentId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                LIST_SQL + " where r.student_id = ? and r.scope = 'STUDENT' and r.status = 'PUBLISHED' order by r.id desc limit 50",
                studentId);
        List<StudentReportVO> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            out.add(toVO(r));
        }
        return out;
    }

    public StudentReportVO detail(Long studentId, Long reportId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                LIST_SQL + " where r.id = ? and r.student_id = ?", reportId, studentId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("报告不存在或无权查看");
        }
        Map<String, Object> row = rows.get(0);
        if (row.get("read_time") == null) {
            jdbc.update("update ai_report set read_time = ? where id = ?", LocalDateTime.now(), reportId);
            row.put("read_time", LocalDateTime.now());
        }
        StudentReportVO vo = toVO(row);
        vo.setUseful(usefulOf(reportId, studentId));
        return vo;
    }

    public void feedback(Long studentId, Long reportId, boolean useful, String reason) {
        Integer n = jdbc.queryForObject("select count(*) from ai_report where id = ? and student_id = ?",
                Integer.class, reportId, studentId);
        if (n == null || n == 0) {
            throw new IllegalArgumentException("报告不存在或无权反馈");
        }
        jdbc.update("insert into ai_report_feedback(report_id, student_id, useful, reason) values (?,?,?,?)",
                reportId, studentId, useful ? 1 : 0, reason == null ? null : reason.substring(0, Math.min(300, reason.length())));
    }

    private Boolean usefulOf(Long reportId, Long studentId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select useful from ai_report_feedback where report_id = ? and student_id = ? order by id desc limit 1",
                reportId, studentId);
        if (rows.isEmpty()) {
            return null;
        }
        Object v = rows.get(0).get("useful");
        return v instanceof Number num && num.intValue() != 0;
    }

    // ================================================================ 教师端批量（后续授课班报告的地基）
    public Map<String, Object> generateForClass(Long teacherId, Long teachingClassId) {
        List<Map<String, Object>> tc = jdbc.queryForList(
                "select id, teacher_id from teaching_class where id = ?", teachingClassId);
        if (tc.isEmpty()) {
            throw new IllegalArgumentException("教学班不存在");
        }
        Long owner = tc.get(0).get("teacher_id") == null ? null : ((Number) tc.get(0).get("teacher_id")).longValue();
        if (teacherId != null && owner != null && !owner.equals(teacherId)) {
            throw new IllegalArgumentException("只能为自己任教的教学班生成报告");
        }
        List<Long> ens = jdbc.queryForList(
                "select id from enrollment where teaching_class_id = ? and status in ('ACTIVE','ENROLLED')", Long.class, teachingClassId);
        int ok = 0;
        int fallback = 0;
        for (Long en : ens) {
            try {
                StudentReportVO vo = generate(null, en, "STAGE");
                if ("DIFY".equals(vo.getSource())) {
                    ok++;
                } else {
                    fallback++;
                }
            } catch (Exception e) {
                log.warn("[Report] 生成失败 enrollmentId={} : {}", en, e.getMessage());
                fallback++;
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", ens.size());
        m.put("dify", ok);
        m.put("fallback", fallback);
        return m;
    }

    // ================================================================ 工具
    private StudentReportVO toVO(Map<String, Object> r) {
        StudentReportVO vo = new StudentReportVO();
        vo.setId(asLong(r.get("id")));
        vo.setEnrollmentId(asLong(r.get("enrollment_id")));
        vo.setTeachingClassId(asLong(r.get("teaching_class_id")));
        vo.setOfferingId(asLong(r.get("offering_id")));
        vo.setCourseName(asString(r.get("course_name")));
        vo.setClassName(asString(r.get("class_name")));
        vo.setTeacherName(asString(r.get("teacher_name")));
        vo.setTermName(asString(r.get("term_name")));
        vo.setReportType(asString(r.get("report_type")));
        vo.setScope(asString(r.get("scope")));
        vo.setRiskLevel(asString(r.get("risk_level")));
        vo.setTotalScore(asDecimal(r.get("total_score")));
        vo.setSummary(asString(r.get("summary")));
        vo.setSource(asString(r.get("source")));
        vo.setRead(r.get("read_time") != null);
        vo.setCreateTime(r.get("create_time") == null ? null : String.valueOf(r.get("create_time")));
        parseContent(asString(r.get("content_json")), vo);
        return vo;
    }

    private void parseContent(String json, StudentReportVO vo) {
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            Map<String, Object> m = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            Object sec = m.get("sections");
            if (sec instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> mm) {
                        StudentReportVO.Section s = new StudentReportVO.Section();
                        s.setTitle(mm.get("title") == null ? null : String.valueOf(mm.get("title")));
                        s.setText(mm.get("text") == null ? null : String.valueOf(mm.get("text")));
                        vo.getSections().add(s);
                    }
                }
            }
            Object act = m.get("actions");
            if (act instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> mm) {
                        Map<String, Object> a = new LinkedHashMap<>();
                        mm.forEach((k, v) -> a.put(String.valueOf(k), v));
                        vo.getActions().add(a);
                    }
                }
            }
            if (vo.getSummary() == null && m.get("summary") != null) {
                vo.setSummary(String.valueOf(m.get("summary")));
            }
        } catch (Exception e) {
            log.warn("[Report] content_json 解析失败: {}", e.getMessage());
        }
    }

    private String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return "app-****";
        }
        return key.substring(0, 7) + "****" + key.substring(key.length() - 3);
    }

    private static Long asLong(Object v) {
        if (v == null) {
            return null;
        }
        return v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v));
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static BigDecimal asDecimal(Object v) {
        if (v == null) {
            return null;
        }
        return v instanceof BigDecimal b ? b : new BigDecimal(String.valueOf(v));
    }
}
