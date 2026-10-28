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
                    if (bad.isEmpty()) {
                        content = composer.composeByDify(fact, text);
                        source = "DIFY";
                        status = "SUCCESS";
                    } else {
                        note.append("Dify 数字回验未通过；");
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
            DashScopeClient.ChatResult res = dashScopeClient.chat(composer.promptSystem(), composer.promptUser(fact));
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
                    DashScopeClient.ChatResult retry = dashScopeClient.chat(composer.promptSystem(), corrective);
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
                if (bad.isEmpty()) {
                    content = composer.composeByDify(fact, text);
                    source = "LLM";
                    status = "SUCCESS";
                } else {
                    note.append("直连模型数字回验未通过（已重写一次）；");
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

        String error = status.equals("SUCCESS") ? null : note.toString();
        Long reportId = persist(fact, type, content, source, runId, tokens);
        runService.usage(runId, "REPORT", tokens, studentId);
        runService.finish(runId, status, source, tokens, error);
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
            ps.setString(i++, props.getModel());
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
