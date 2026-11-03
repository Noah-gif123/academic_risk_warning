package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.config.AiProperties;
import com.example.academic_risk_warning.vo.ai.AdminClassFact;
import com.example.academic_risk_warning.vo.ai.ClassFact;
import com.example.academic_risk_warning.vo.ai.GroupReportVO;
import com.example.academic_risk_warning.vo.ai.OfferingFact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 群体报告（M8）：教学班报告与行政班报告。
 *
 * <p>与学生报告同一套哲学：**数字由规则算，模型只写结论与建议**；
 * 通道三层（Dify 群体工作流 → 直连模型 → 规则模板），任何一层失败都能出报告。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupReportService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final GroupFactBuilder factBuilder;
    private final RetrievalService retrievalService;
    private final ReportComposer composer;
    private final DifyClient difyClient;
    private final DashScopeClient dashScopeClient;
    private final AiRunService runService;
    private final AiProperties props;
    private final JdbcTemplate jdbc;

    // ============================================================ 教学班报告
    public GroupReportVO generateClassReport(Long teachingClassId, Long operatorId) {
        ClassFact fact = factBuilder.buildClass(teachingClassId);
        String title = nz(fact.getClassName()) + " · 班级学情报告（" + nz(fact.getTermName()) + "）";
        ReportContent content = null;
        String source = "TEMPLATE";
        String status = "FALLBACK";
        StringBuilder note = new StringBuilder();
        Long runId = runService.start("GROUP_REPORT", "TEACHING_CLASS", teachingClassId, operatorId);
        long t0 = System.currentTimeMillis();
        int tokens = 0;

        // R1：群体报告也给「参考依据」——教师看到的是"本班薄弱项对应的课程资料"
        List<RetrievalService.Hit> evidence = retrieveEvidence(fact.getCourseId(), fact.getCourseName());
        String evidenceBlock = evidenceBlock(evidence);
        String userPrompt = classPrompt(fact) + evidenceBlock;
        String systemPrompt = groupSystemPrompt("教学班");

        if (props.isEnabled() && props.getGroupReportAppKey() != null && !props.getGroupReportAppKey().isBlank()) {
            Map<String, Object> inputs = classInputs(fact);
            if (!evidenceBlock.isEmpty()) {
                inputs.put("facts", String.valueOf(inputs.get("facts")) + evidenceBlock);
            }
            DifyClient.WorkflowResult res = difyClient.runWorkflow(props.getGroupReportAppKey(),
                    inputs, "teacher-" + operatorId);
            tokens += res.getTotalTokens();
            if (res.isOk()) {
                String text = pickText(res.getOutputs());
                List<String> bad = composer.invalidNumbersAgainst(text, fact);
                runService.step(runId, 1, "dify-group-workflow", "class-report", text,
                        bad.isEmpty() ? "PASS" : "WARN",
                        bad.isEmpty() ? null : "越界数字 " + bad.stream().distinct().limit(6).toList(),
                        System.currentTimeMillis() - t0);
                if (bad.isEmpty() && composer.looksLikeGroupReport(text)) {
                    content = parseContent(text);
                    source = "DIFY";
                    status = "SUCCESS";
                } else {
                    note.append("Dify 输出不可用；");
                }
            } else {
                note.append("Dify 失败：").append(res.getError()).append("；");
            }
        } else {
            note.append("未配置群体报告工作流；");
        }

        if (content == null && props.directReady()) {
            // 群体报告要求四段 350~550 字，推理型模型容易把 2400 的预算全烧在思考上，这里给到 3600
            DashScopeClient.ChatResult res = dashScopeClient.chat(systemPrompt, userPrompt, 0.4, 3600);
            tokens += res.getTotalTokens();
            long t1 = System.currentTimeMillis();
            if (res.isOk()) {
                List<String> bad = composer.invalidNumbersAgainst(res.getText(), fact);
                runService.step(runId, 2, "direct-chat", "class-report", res.getText(),
                        bad.isEmpty() ? "PASS" : "WARN",
                        bad.isEmpty() ? null : "越界数字 " + bad.stream().distinct().limit(6).toList(),
                        System.currentTimeMillis() - t1);
                if (bad.isEmpty() && composer.looksLikeGroupReport(res.getText())) {
                    content = parseContent(res.getText());
                    source = "LLM";
                    status = "SUCCESS";
                } else {
                    note.append("直连输出不可用；");
                }
            } else {
                note.append("直连失败：").append(res.getError()).append("；");
            }
        }

        if (content == null) {
            content = classTemplate(fact);
            note.append("已使用规则模板");
        }
        appendEvidence(content, evidence);

        Long id = persist("CLASS", teachingClassId, fact.getTeachingClassId(), null,
                fact.getOfferingId(), fact.getCourseId(), fact.getTermId(), title,
                fact, content, source, runId, tokens, operatorId);
        runService.usage(runId, "GROUP_REPORT", tokens, operatorId);
        runService.finish(runId, status, source, tokens, status.equals("SUCCESS") ? null : note.toString());
        return detail(id);
    }

    // ============================================================ 行政班报告
    public GroupReportVO generateAdminClassReport(Long adminClassId, Long operatorId) {
        AdminClassFact fact = factBuilder.buildAdminClass(adminClassId);
        String title = nz(fact.getClassName()) + " · 行政班学情报告（" + nz(fact.getTermName()) + "）";
        ReportContent content = null;
        String source = "TEMPLATE";
        String status = "FALLBACK";
        StringBuilder note = new StringBuilder();
        Long runId = runService.start("GROUP_REPORT", "ADMIN_CLASS", adminClassId, operatorId);
        long t0 = System.currentTimeMillis();
        int tokens = 0;

        // P1：行政班也带「参考依据」——取风险最集中那门课的资料（行政班跨多门课，只挑最急的一门）
        Long riskCourseId = null;
        String riskCourseName = null;
        if (!fact.getCourseRisks().isEmpty()) {
            riskCourseId = fact.getCourseRisks().get(0).getCourseId();
            riskCourseName = fact.getCourseRisks().get(0).getCourseName();
        }
        List<RetrievalService.Hit> evidence = retrieveEvidence(riskCourseId, riskCourseName);
        String evidenceBlock = evidenceBlock(evidence);

        String systemPrompt = groupSystemPrompt("行政班");
        String userPrompt = adminPrompt(fact) + evidenceBlock;

        if (props.isEnabled() && props.getGroupReportAppKey() != null && !props.getGroupReportAppKey().isBlank()) {
            Map<String, Object> inputs = adminInputs(fact);
            if (!evidenceBlock.isEmpty()) {
                inputs.put("facts", String.valueOf(inputs.get("facts")) + evidenceBlock);
            }
            DifyClient.WorkflowResult res = difyClient.runWorkflow(props.getGroupReportAppKey(),
                    inputs, "counselor-" + operatorId);
            tokens += res.getTotalTokens();
            if (res.isOk()) {
                String text = pickText(res.getOutputs());
                List<String> bad = composer.invalidNumbersAgainst(text, fact);
                runService.step(runId, 1, "dify-group-workflow", "admin-report", text,
                        bad.isEmpty() ? "PASS" : "WARN",
                        bad.isEmpty() ? null : "越界数字 " + bad.stream().distinct().limit(6).toList(),
                        System.currentTimeMillis() - t0);
                if (bad.isEmpty() && composer.looksLikeGroupReport(text)) {
                    content = parseContent(text);
                    source = "DIFY";
                    status = "SUCCESS";
                } else {
                    note.append("Dify 输出不可用；");
                }
            } else {
                note.append("Dify 失败：").append(res.getError()).append("；");
            }
        } else {
            note.append("未配置群体报告工作流；");
        }

        if (content == null && props.directReady()) {
            // 群体报告要求四段 350~550 字，推理型模型容易把 2400 的预算全烧在思考上，这里给到 3600
            DashScopeClient.ChatResult res = dashScopeClient.chat(systemPrompt, userPrompt, 0.4, 3600);
            tokens += res.getTotalTokens();
            long t1 = System.currentTimeMillis();
            if (res.isOk()) {
                List<String> bad = composer.invalidNumbersAgainst(res.getText(), fact);
                runService.step(runId, 2, "direct-chat", "admin-report", res.getText(),
                        bad.isEmpty() ? "PASS" : "WARN",
                        bad.isEmpty() ? null : "越界数字 " + bad.stream().distinct().limit(6).toList(),
                        System.currentTimeMillis() - t1);
                if (bad.isEmpty() && composer.looksLikeGroupReport(res.getText())) {
                    content = parseContent(res.getText());
                    source = "LLM";
                    status = "SUCCESS";
                } else {
                    note.append("直连输出不可用；");
                }
            } else {
                note.append("直连失败：").append(res.getError()).append("；");
            }
        }

        if (content == null) {
            content = adminTemplate(fact);
            note.append("已使用规则模板");
        }
        appendEvidence(content, evidence);

        Long id = persist("ADMIN_CLASS", adminClassId, null, adminClassId,
                null, null, fact.getTermId(), title, fact, content, source, runId, tokens, operatorId);
        runService.usage(runId, "GROUP_REPORT", tokens, operatorId);
        runService.finish(runId, status, source, tokens, status.equals("SUCCESS") ? null : note.toString());
        return detail(id);
    }

    // ============================================================ 课程报告（一门课 × 多个教学班）
    /**
     * 教师名下一门课<b>全部教学班</b>的汇总报告（P3）。
     *
     * <p>与教学班报告的区别只在事实包：这边是把同一 offering 下各班汇总，
     * 所以结论侧重"哪个班最吃力 / 全班共同的薄弱项"，其余三层通道与校验完全复用。</p>
     */
    public GroupReportVO generateOfferingReport(Long offeringId, Long operatorId) {
        OfferingFact fact = factBuilder.buildOffering(offeringId, operatorId);
        String title = nz(fact.getCourseName()) + " · 课程学情报告（" + nz(fact.getTermName())
                + " · " + nzf(fact.getClassCount()) + " 个教学班）";
        ReportContent content = null;
        String source = "TEMPLATE";
        String status = "FALLBACK";
        StringBuilder note = new StringBuilder();
        Long runId = runService.start("GROUP_REPORT", "OFFERING", offeringId, operatorId);
        long t0 = System.currentTimeMillis();
        int tokens = 0;

        List<RetrievalService.Hit> evidence = retrieveEvidence(fact.getCourseId(), fact.getCourseName());
        String evidenceBlock = evidenceBlock(evidence);
        String systemPrompt = groupSystemPrompt("课程");
        String userPrompt = offeringPrompt(fact) + evidenceBlock;

        if (props.isEnabled() && props.getGroupReportAppKey() != null && !props.getGroupReportAppKey().isBlank()) {
            Map<String, Object> inputs = offeringInputs(fact);
            if (!evidenceBlock.isEmpty()) {
                inputs.put("facts", String.valueOf(inputs.get("facts")) + evidenceBlock);
            }
            DifyClient.WorkflowResult res = difyClient.runWorkflow(props.getGroupReportAppKey(),
                    inputs, "teacher-" + operatorId);
            tokens += res.getTotalTokens();
            if (res.isOk()) {
                String text = pickText(res.getOutputs());
                List<String> bad = composer.invalidNumbersAgainst(text, fact);
                runService.step(runId, 1, "dify-group-workflow", "offering-report", text,
                        bad.isEmpty() ? "PASS" : "WARN",
                        bad.isEmpty() ? null : "越界数字 " + bad.stream().distinct().limit(6).toList(),
                        System.currentTimeMillis() - t0);
                if (bad.isEmpty() && composer.looksLikeGroupReport(text)) {
                    content = parseContent(text);
                    source = "DIFY";
                    status = "SUCCESS";
                } else {
                    note.append("Dify 输出不可用；");
                }
            } else {
                note.append("Dify 失败：").append(res.getError()).append("；");
            }
        } else {
            note.append("未配置群体报告工作流；");
        }

        if (content == null && props.directReady()) {
            // 群体报告要求四段 350~550 字，推理型模型容易把 2400 的预算全烧在思考上，这里给到 3600
            DashScopeClient.ChatResult res = dashScopeClient.chat(systemPrompt, userPrompt, 0.4, 3600);
            tokens += res.getTotalTokens();
            long t1 = System.currentTimeMillis();
            if (res.isOk()) {
                List<String> bad = composer.invalidNumbersAgainst(res.getText(), fact);
                runService.step(runId, 2, "direct-chat", "offering-report", res.getText(),
                        bad.isEmpty() ? "PASS" : "WARN",
                        bad.isEmpty() ? null : "越界数字 " + bad.stream().distinct().limit(6).toList(),
                        System.currentTimeMillis() - t1);
                if (bad.isEmpty() && composer.looksLikeGroupReport(res.getText())) {
                    content = parseContent(res.getText());
                    source = "LLM";
                    status = "SUCCESS";
                } else {
                    note.append("直连输出不可用；");
                }
            } else {
                note.append("直连失败：").append(res.getError()).append("；");
            }
        }

        if (content == null) {
            content = offeringTemplate(fact);
            note.append("已使用规则模板");
        }
        appendEvidence(content, evidence);

        Long id = persist("OFFERING", offeringId, null, null, offeringId,
                fact.getCourseId(), fact.getTermId(), title, fact, content, source, runId, tokens, operatorId);
        runService.usage(runId, "GROUP_REPORT", tokens, operatorId);
        runService.finish(runId, status, source, tokens, status.equals("SUCCESS") ? null : note.toString());
        return detail(id);
    }

    // ============================================================ 权限
    /** 教师只能操作自己任教的教学班 */
    public void assertTeacherOwns(Long teachingClassId, Long teacherId) {
        List<Long> owners = jdbc.queryForList(
                "select teacher_id from teaching_class where id = ?", Long.class, teachingClassId);
        if (owners.isEmpty()) {
            throw new BizException("教学班不存在");
        }
        if (teacherId != null && !teacherId.equals(owners.get(0))) {
            throw new BizException("只能查看自己任教的教学班");
        }
    }

    /** 教师只能对自己任教的课程出课程总报告（该课程下至少有一个自己的教学班） */
    public void assertTeacherOwnsOffering(Long offeringId, Long teacherId) {
        Integer n = jdbc.queryForObject(
                "select count(*) from teaching_class where offering_id = ? and (? is null or teacher_id = ?)",
                Integer.class, offeringId, teacherId, teacherId);
        if (n == null || n == 0) {
            throw new BizException("只能查看自己任教的课程");
        }
    }

    /** 辅导员只能操作自己带的行政班 */
    public void assertCounselorOwns(Long adminClassId, Long counselorId) {
        Integer n = jdbc.queryForObject(
                "select count(*) from counselor_class where admin_class_id = ? and (? is null or counselor_id = ?)",
                Integer.class, adminClassId, counselorId, counselorId);
        if (n == null || n == 0) {
            throw new BizException("不在你负责的行政班范围内");
        }
    }

    // ============================================================ 查询
    public GroupReportVO latest(String scope, Long targetId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select id from ai_group_report where scope = ? and target_id = ? and status = 'PUBLISHED' order by id desc limit 1",
                scope, targetId);
        if (rows.isEmpty()) {
            return null;
        }
        return detail(((Number) rows.get(0).get("id")).longValue());
    }

    public List<GroupReportVO> list(String scope, Long targetId, int limit) {
        List<GroupReportVO> out = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList(
                "select id from ai_group_report where scope = ? and target_id = ? order by id desc limit ?",
                scope, targetId, limit)) {
            out.add(detail(((Number) row.get("id")).longValue()));
        }
        return out;
    }

    public GroupReportVO detail(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select g.id, g.scope, g.target_id, g.teaching_class_id, g.admin_class_id, g.title, g.summary, "
                        + "g.content_json, g.source, g.create_time, "
                        + "(select max(rs.last_eval_time) from risk_state rs "
                        + "   where (g.teaching_class_id is not null and rs.teaching_class_id = g.teaching_class_id) "
                        + "      or (g.scope = 'OFFERING' and g.offering_id is not null "
                        + "          and rs.teaching_class_id in (select tc.id from teaching_class tc "
                        + "                                       where tc.offering_id = g.offering_id))) as last_eval_time "
                        + "from ai_group_report g where g.id = ?", id);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("报告不存在：" + id);
        }
        Map<String, Object> r = rows.get(0);
        GroupReportVO vo = new GroupReportVO();
        vo.setId(((Number) r.get("id")).longValue());
        vo.setScope(String.valueOf(r.get("scope")));
        vo.setTargetId(r.get("target_id") == null ? null : ((Number) r.get("target_id")).longValue());
        vo.setTeachingClassId(r.get("teaching_class_id") == null ? null : ((Number) r.get("teaching_class_id")).longValue());
        vo.setAdminClassId(r.get("admin_class_id") == null ? null : ((Number) r.get("admin_class_id")).longValue());
        vo.setTitle(String.valueOf(r.get("title")));
        vo.setSummary(r.get("summary") == null ? null : String.valueOf(r.get("summary")));
        vo.setSource(String.valueOf(r.get("source")));
        vo.setCreateTime(r.get("create_time") == null ? null : String.valueOf(r.get("create_time")));
        // Q2：班级数据重判过就标记过期，避免"旧报告 + 新判定"同时展示
        java.time.LocalDateTime created = toLdt(r.get("create_time"));
        java.time.LocalDateTime lastEval = toLdt(r.get("last_eval_time"));
        if (created != null && lastEval != null && lastEval.isAfter(created)) {
            vo.setStale(true);
            vo.setStaleReason("该报告生成于 " + created.toLocalDate() + "，班级数据在 "
                    + lastEval.toString().replace('T', ' ').substring(0, 16) + " 重新判定过，建议重新生成");
        }
        parseInto(String.valueOf(r.get("content_json")), vo);
        return vo;
    }

    // ============================================================ 落库
    private Long persist(String scope, Long targetId, Long teachingClassId, Long adminClassId,
                         Long offeringId, Long courseId, Long termId, String title,
                         Object fact, ReportContent content, String source, Long runId, int tokens, Long operatorId) {
        String factJson = toJson(fact);
        String contentJson = toJson(content.toSections());
        BigDecimal cost = BigDecimal.valueOf(props.costOf(tokens));
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    insert into ai_group_report(scope, target_id, teaching_class_id, admin_class_id, offering_id, course_id, term_id,
                                                title, fact_json, content_json, summary, status, source, model, app_id,
                                                prompt_version, run_id, tokens, cost, create_by, publish_time)
                    values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, scope);
            ps.setLong(i++, targetId);
            setNullableLong(ps, i++, teachingClassId);
            setNullableLong(ps, i++, adminClassId);
            setNullableLong(ps, i++, offeringId);
            setNullableLong(ps, i++, courseId);
            setNullableLong(ps, i++, termId);
            ps.setString(i++, title);
            ps.setString(i++, factJson);
            ps.setString(i++, contentJson);
            ps.setString(i++, content.getSummary());
            ps.setString(i++, "PUBLISHED");
            ps.setString(i++, source);
            ps.setString(i++, props.getModel());
            ps.setString(i++, props.getGroupReportAppKey() == null || props.getGroupReportAppKey().isBlank()
                    ? null : maskKey(props.getGroupReportAppKey()));
            ps.setString(i++, "group-report-v1");
            setNullableLong(ps, i++, runId);
            ps.setInt(i++, tokens);
            ps.setBigDecimal(i++, cost);
            setNullableLong(ps, i++, operatorId);
            ps.setObject(i, LocalDateTime.now());
            return ps;
        }, kh);
        return kh.getKey() == null ? null : kh.getKey().longValue();
    }

    private static void setNullableLong(PreparedStatement ps, int idx, Long v) throws java.sql.SQLException {
        if (v == null) {
            ps.setNull(idx, java.sql.Types.BIGINT);
        } else {
            ps.setLong(idx, v);
        }
    }

    // ============================================================ 提示词
    private String groupSystemPrompt(String kind) {
        String heads = switch (kind) {
            case "教学班" -> "【风险概览】【共性难点】【重点关注】【教学建议】";
            case "课程" -> "【整体概览】【教学班对比】【共性难点】【教学建议】";
            default -> "【班级画像】【集中风险课程】【多门预警学生】【干预建议】";
        };
        return """
                你是高校学业预警系统里的「群体学情研判助手」，服务对象是教师与辅导员。
                必须遵守：
                1. 只能使用用户给出的数字，禁止自己计算、换算或编造任何数字、比例、排名；
                2. 四段结构，段落标题只能是：%s；
                3. 面向教师/辅导员，语气专业、结论先行，每段 1 到 3 句；
                4. 不使用 Markdown 星号、表格、代码块；数字只挑最关键的 5 个以内；
                5. 总字数 350 到 550 字，最后一段给出 3 条可直接执行的动作；
                6. 直接输出正文，不要复述规则、不要输出思考过程。
                """.formatted(heads);
    }

    private String classPrompt(ClassFact f) {
        StringBuilder sb = new StringBuilder();
        sb.append("课程：").append(nz(f.getCourseName())).append("（").append(nz(f.getCourseType()))
                .append("，").append(nzf(f.getCredit())).append(" 学分，").append(nzf(f.getPlannedWeeks())).append(" 周）\n");
        sb.append("教学班：").append(nz(f.getClassName())).append("，任课教师：").append(nz(f.getTeacherName()))
                .append("，学期：").append(nz(f.getTermName())).append("\n");
        sb.append("人数：").append(nzf(f.getStudentCount()))
                .append("；风险分布：红灯 ").append(nzf(f.getRedCount()))
                .append("、黄灯 ").append(nzf(f.getYellowCount()))
                .append("、绿灯 ").append(nzf(f.getGreenCount())).append("\n");
        sb.append("总分：班级平均 ").append(num(f.getAvgScore()))
                .append("，最低 ").append(num(f.getMinScore()))
                .append("，最高 ").append(num(f.getMaxScore()))
                .append("，及格线 ").append(nzf(f.getPassScore())).append("\n");
        sb.append("考勤：缺勤超上限 ").append(nzf(f.getAbsentOverLimitCount()))
                .append(" 人，人均缺勤 ").append(num(f.getAvgAbsent()))
                .append(" 次，最多缺勤 ").append(nzf(f.getMaxAbsent())).append(" 次\n");
        sb.append("薄弱分组（正确率由低到高）：");
        for (ClassFact.Dim d : f.getWeakGroups()) {
            sb.append(d.getName()).append("=").append(pct(d.getRate())).append("；");
        }
        sb.append("\n薄弱题型：");
        for (ClassFact.Dim d : f.getWeakTypes()) {
            sb.append(d.getName()).append("=").append(pct(d.getRate())).append("；");
        }
        sb.append("\n低分分项：");
        for (ClassFact.Item it : f.getWeakItems()) {
            sb.append(it.getName()).append("=").append(num(it.getAvgScore())).append("；");
        }
        sb.append("\n未关闭预警事件：").append(nzf(f.getActiveAlertCount())).append(" 条\n");
        sb.append("\n请据此写这个教学班的学情研判。");
        return sb.toString();
    }

    private Map<String, Object> classInputs(ClassFact f) {
        Map<String, Object> in = new LinkedHashMap<>();
        in.put("facts", classPrompt(f));
        in.put("kind", "class");
        in.put("course", nz(f.getCourseName()));
        in.put("class_name", nz(f.getClassName()));
        in.put("student_count", nzf(f.getStudentCount()));
        in.put("red", nzf(f.getRedCount()));
        in.put("yellow", nzf(f.getYellowCount()));
        in.put("green", nzf(f.getGreenCount()));
        in.put("avg_score", num(f.getAvgScore()));
        return in;
    }

    private String adminPrompt(AdminClassFact f) {
        StringBuilder sb = new StringBuilder();
        sb.append("行政班：").append(nz(f.getClassName())).append("（").append(nzf(f.getGradeYear()))
                .append(" 级，").append(nz(f.getMajorName())).append("），辅导员：").append(nz(f.getCounselorName()))
                .append("，学期：").append(nz(f.getTermName())).append("\n");
        sb.append("规模：学生 ").append(nzf(f.getStudentCount())).append(" 人，本学期待评课程 ")
                .append(nzf(f.getCourseCount())).append(" 门，选课记录 ").append(nzf(f.getEnrollmentCount())).append(" 条\n");
        sb.append("风险分布：红灯 ").append(nzf(f.getRedCount()))
                .append("、黄灯 ").append(nzf(f.getYellowCount()))
                .append("、绿灯 ").append(nzf(f.getGreenCount()))
                .append("，挂科风险率 ").append(num(f.getFailRate())).append("%\n");
        sb.append("平均总分：").append(num(f.getAvgScore())).append("；未关闭预警事件：")
                .append(nzf(f.getActiveAlertCount())).append(" 条\n");
        sb.append("集中风险课程：");
        for (AdminClassFact.CourseRisk c : f.getCourseRisks()) {
            sb.append(c.getCourseName()).append("（红 ").append(nzf(c.getRed()))
                    .append("、黄 ").append(nzf(c.getYellow())).append("，风险率 ")
                    .append(num(c.getFailRate())).append("%）；");
        }
        sb.append("\n多门预警学生（≥2 门）：");
        for (AdminClassFact.Student s : f.getMultiRiskStudents()) {
            sb.append(s.getStudentName()).append("（").append(nzf(s.getRiskCount())).append(" 门：")
                    .append(nz(s.getRiskCourses())).append("）；");
        }
        sb.append("\n请据此写这个行政班的学情研判与干预建议。");
        return sb.toString();
    }

    private Map<String, Object> adminInputs(AdminClassFact f) {
        Map<String, Object> in = new LinkedHashMap<>();
        in.put("facts", adminPrompt(f));
        in.put("kind", "admin");
        in.put("class_name", nz(f.getClassName()));
        in.put("student_count", nzf(f.getStudentCount()));
        in.put("red", nzf(f.getRedCount()));
        in.put("yellow", nzf(f.getYellowCount()));
        in.put("green", nzf(f.getGreenCount()));
        in.put("fail_rate", num(f.getFailRate()));
        return in;
    }

    private String offeringPrompt(OfferingFact f) {
        StringBuilder sb = new StringBuilder();
        sb.append("课程：").append(nz(f.getCourseName())).append("（").append(nz(f.getCourseType()))
                .append("，").append(nzf(f.getCredit())).append(" 学分，").append(nzf(f.getPlannedWeeks()))
                .append(" 周），任课教师：").append(nz(f.getTeacherName()))
                .append("，学期：").append(nz(f.getTermName())).append("\n");
        sb.append("范围：我任教的 ").append(nzf(f.getClassCount())).append(" 个教学班，合计学生 ")
                .append(nzf(f.getStudentCount())).append(" 人\n");
        sb.append("风险分布：红灯 ").append(nzf(f.getRedCount()))
                .append("、黄灯 ").append(nzf(f.getYellowCount()))
                .append("、绿灯 ").append(nzf(f.getGreenCount()))
                .append("，红灯占比 ").append(num(f.getFailRate())).append("%\n");
        sb.append("总分：全课程平均 ").append(num(f.getAvgScore()))
                .append("，最低 ").append(num(f.getMinScore()))
                .append("，最高 ").append(num(f.getMaxScore()))
                .append("，及格线 ").append(nzf(f.getPassScore())).append("\n");
        sb.append("考勤：缺勤超上限 ").append(nzf(f.getAbsentOverLimitCount()))
                .append(" 人；未关闭预警事件 ").append(nzf(f.getActiveAlertCount())).append(" 条\n");
        sb.append("教学班横向对比（按红黄灯人次从多到少）：");
        for (OfferingFact.ClassRow c : f.getClasses()) {
            sb.append(c.getClassName()).append("（").append(nzf(c.getStudentCount())).append(" 人，红 ")
                    .append(nzf(c.getRed())).append("、黄 ").append(nzf(c.getYellow()))
                    .append("，平均 ").append(num(c.getAvgScore()))
                    .append("，最低 ").append(num(c.getMinScore())).append("）；");
        }
        sb.append("\n全课程共性难点（分组，正确率由低到高）：");
        for (OfferingFact.Dim d : f.getWeakGroups()) {
            sb.append(d.getName()).append("=").append(pct(d.getRate())).append("；");
        }
        sb.append("\n最弱题型：");
        for (OfferingFact.Dim d : f.getWeakTypes()) {
            sb.append(d.getName()).append("=").append(pct(d.getRate())).append("；");
        }
        sb.append("\n重点关注（跨班，最多 10 人）：");
        for (OfferingFact.Student s : f.getWatchList()) {
            sb.append(s.getStudentName()).append("（").append(nz(s.getClassName())).append("，")
                    .append("RED".equals(s.getRiskLevel()) ? "红灯" : "黄灯").append("，")
                    .append(num(s.getTotalScore())).append(" 分，").append(nz(s.getReason())).append("）；");
        }
        sb.append("\n请据此写这门课整体的学情研判，重点说清哪个教学班最吃力、各班共同的薄弱点");
        sb.append("，以及我作为任课教师下一步怎么调整教学。");
        return sb.toString();
    }

    private Map<String, Object> offeringInputs(OfferingFact f) {
        Map<String, Object> in = new LinkedHashMap<>();
        in.put("facts", offeringPrompt(f));
        in.put("kind", "offering");
        in.put("course", nz(f.getCourseName()));
        in.put("term", nz(f.getTermName()));
        in.put("class_count", nzf(f.getClassCount()));
        in.put("student_count", nzf(f.getStudentCount()));
        in.put("red", nzf(f.getRedCount()));
        in.put("yellow", nzf(f.getYellowCount()));
        in.put("green", nzf(f.getGreenCount()));
        in.put("fail_rate", num(f.getFailRate()));
        in.put("avg_score", num(f.getAvgScore()));
        return in;
    }

    // ============================================================ 模板兜底
    private ReportContent classTemplate(ClassFact f) {
        List<ReportContent.Section> sections = new ArrayList<>();
        String overview = "（" + nz(f.getTermName()) + "）本班 " + nzf(f.getStudentCount()) + " 人，红灯 " + nzf(f.getRedCount())
                + " 人、黄灯 " + nzf(f.getYellowCount()) + " 人、绿灯 " + nzf(f.getGreenCount())
                + " 人；班级平均 " + num(f.getAvgScore()) + " 分，及格线 " + nzf(f.getPassScore()) + " 分。";
        sections.add(new ReportContent.Section("风险概览", overview));

        StringBuilder weak = new StringBuilder();
        if (!f.getWeakGroups().isEmpty()) {
            weak.append("正确率最低的分组：");
            int i = 1;
            for (ClassFact.Dim d : f.getWeakGroups()) {
                if (i > 3) {
                    break;
                }
                weak.append(i++).append("）「").append(d.getName()).append("」").append(pct(d.getRate())).append("；");
            }
        }
        if (!f.getWeakTypes().isEmpty()) {
            ClassFact.Dim t = f.getWeakTypes().get(0);
            weak.append("最弱题型是").append(t.getName()).append("（").append(pct(t.getRate())).append("）。");
        }
        sections.add(new ReportContent.Section("共性难点", weak.length() == 0 ? "暂无作答数据，无法判断共性难点。" : weak.toString()));

        StringBuilder watch = new StringBuilder();
        int red = nzfInt(f.getRedCount());
        int yellow = nzfInt(f.getYellowCount());
        if (red + yellow == 0) {
            watch.append("当前没有红黄灯学生，保持现有教学节奏即可。");
        } else {
            watch.append("需重点关注 ").append(red + yellow).append(" 人：");
            int i = 1;
            for (ClassFact.Student s : f.getWatchList()) {
                if (i > 5 || "GREEN".equals(s.getRiskLevel())) {
                    break;
                }
                watch.append(i++).append("）").append(s.getStudentName()).append("（")
                        .append("RED".equals(s.getRiskLevel()) ? "红灯" : "黄灯").append("，")
                        .append(num(s.getTotalScore())).append(" 分，").append(nz(s.getReason())).append("）；");
            }
        }
        sections.add(new ReportContent.Section("重点关注", watch.toString()));

        sections.add(new ReportContent.Section("教学建议",
                "一是针对正确率最低的分组安排一次课堂回顾；二是对红灯学生逐一约谈并在系统登记干预；"
                        + "三是核对未登分项，避免成绩不齐导致判定偏差。"));

        return new ReportContent(summaryOf(sections), sections);
    }

    private ReportContent adminTemplate(AdminClassFact f) {
        List<ReportContent.Section> sections = new ArrayList<>();
        sections.add(new ReportContent.Section("班级画像",
                nz(f.getClassName()) + " 共 " + nzf(f.getStudentCount()) + " 人（" + nz(f.getTermName())
                        + "），本学期待评课程 "
                        + nzf(f.getCourseCount()) + " 门，挂科风险率 " + num(f.getFailRate())
                        + "%，平均总分 " + num(f.getAvgScore()) + "。"));

        StringBuilder courses = new StringBuilder();
        if (f.getCourseRisks().isEmpty()) {
            courses.append("暂无集中风险课程。");
        } else {
            courses.append("风险最集中的课程：");
            int i = 1;
            for (AdminClassFact.CourseRisk c : f.getCourseRisks()) {
                courses.append(i++).append("）").append(c.getCourseName())
                        .append("（红 ").append(nzf(c.getRed())).append(" 人、黄 ").append(nzf(c.getYellow()))
                        .append(" 人）；");
            }
        }
        sections.add(new ReportContent.Section("集中风险课程", courses.toString()));

        StringBuilder multi = new StringBuilder();
        if (f.getMultiRiskStudents().isEmpty()) {
            multi.append("没有学生出现两门及以上预警。");
        } else {
            multi.append("需优先谈话 ").append(f.getMultiRiskStudents().size()).append(" 人：");
            int i = 1;
            for (AdminClassFact.Student s : f.getMultiRiskStudents()) {
                if (i > 5) {
                    break;
                }
                multi.append(i++).append("）").append(s.getStudentName()).append("（")
                        .append(nzf(s.getRiskCount())).append(" 门，").append(nz(s.getRiskCourses())).append("）；");
            }
        }
        sections.add(new ReportContent.Section("多门预警学生", multi.toString()));

        sections.add(new ReportContent.Section("干预建议",
                "一是先找多门预警学生谈话并登记干预记录；二是与集中风险课程的任课教师对齐，确认是否需要统一答疑；"
                        + "三是在系统中跟进干预后的等级变化，两周后复核一次。"));
        return new ReportContent(summaryOf(sections), sections);
    }

    private ReportContent offeringTemplate(OfferingFact f) {
        List<ReportContent.Section> sections = new ArrayList<>();
        sections.add(new ReportContent.Section("整体概览",
                "（" + nz(f.getTermName()) + "）" + nz(f.getCourseName()) + " 我任教 "
                        + nzf(f.getClassCount()) + " 个教学班、合计 " + nzf(f.getStudentCount()) + " 人；"
                        + "红灯 " + nzf(f.getRedCount()) + " 人、黄灯 " + nzf(f.getYellowCount())
                        + " 人、绿灯 " + nzf(f.getGreenCount()) + " 人，红灯占比 " + num(f.getFailRate()) + "%；"
                        + "全课程平均 " + num(f.getAvgScore()) + " 分，及格线 " + nzf(f.getPassScore()) + " 分。"));

        StringBuilder cmp = new StringBuilder();
        if (f.getClasses().isEmpty()) {
            cmp.append("暂无可对比的教学班。");
        } else {
            OfferingFact.ClassRow worst = f.getClasses().get(0);
            cmp.append("最吃力的是「").append(worst.getClassName()).append("」（红 ")
                    .append(nzf(worst.getRed())).append("、黄 ").append(nzf(worst.getYellow()))
                    .append("，平均 ").append(num(worst.getAvgScore()))
                    .append("，最低 ").append(num(worst.getMinScore())).append(" 分）。各班情况：");
            int i = 1;
            for (OfferingFact.ClassRow c : f.getClasses()) {
                cmp.append(i++).append("）").append(c.getClassName())
                        .append(" ").append(nzf(c.getStudentCount())).append(" 人，红 ")
                        .append(nzf(c.getRed())).append("、黄 ").append(nzf(c.getYellow()))
                        .append("，平均 ").append(num(c.getAvgScore())).append("；");
            }
        }
        sections.add(new ReportContent.Section("教学班对比", cmp.toString()));

        StringBuilder weak = new StringBuilder();
        if (!f.getWeakGroups().isEmpty()) {
            weak.append("全课程正确率最低的分组：");
            int i = 1;
            for (OfferingFact.Dim d : f.getWeakGroups()) {
                if (i > 3) {
                    break;
                }
                weak.append(i++).append("）「").append(d.getName()).append("」").append(pct(d.getRate())).append("；");
            }
        }
        if (!f.getWeakTypes().isEmpty()) {
            weak.append("最弱题型是").append(f.getWeakTypes().get(0).getName())
                    .append("（").append(pct(f.getWeakTypes().get(0).getRate())).append("）。");
        }
        sections.add(new ReportContent.Section("共性难点",
                weak.length() == 0 ? "暂无作答数据，无法判断共性难点。" : weak.toString()));

        sections.add(new ReportContent.Section("教学建议",
                "一是把全课程共同薄弱的分组排进下一轮复习，多个班同步讲；二是对最吃力那个教学班单独加一次答疑，"
                        + "并核对该班缺勤超上限的 " + nzf(f.getAbsentOverLimitCount()) + " 人；"
                        + "三是跨班红黄灯学生统一约谈，处理完在系统登记干预。"));

        return new ReportContent(summaryOf(sections), sections);
    }

    private String summaryOf(List<ReportContent.Section> sections) {
        String s = sections.isEmpty() ? "" : sections.get(0).getText();
        return s.length() <= 290 ? s : s.substring(0, 289) + "…";
    }

    // ============================================================ 解析
    private ReportContent parseContent(String text) {
        List<ReportContent.Section> sections = new ArrayList<>();
        for (ReportComposer.GroupSection gs : composer.parseGroupSections(text)) {
            sections.add(new ReportContent.Section(gs.getTitle(), gs.getText()));
        }
        return new ReportContent(summaryOf(sections), sections);
    }

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
        return outputs.values().stream().filter(Objects::nonNull).map(String::valueOf)
                .filter(s -> s.length() > 40).findFirst().orElse(null);
    }

    private void parseInto(String json, GroupReportVO vo) {
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
                        GroupReportVO.Section s = new GroupReportVO.Section();
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
        } catch (Exception e) {
            log.warn("[GroupReport] content 解析失败: {}", e.getMessage());
        }
    }

    /** 各种 JDBC 时间类型统一转 LocalDateTime */
    private static java.time.LocalDateTime toLdt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof java.time.LocalDateTime t) {
            return t;
        }
        if (v instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        try {
            return java.time.LocalDateTime.parse(String.valueOf(v).replace(' ', 'T'));
        } catch (Exception e) {
            return null;
        }
    }
    // ============================================================ R1：课程资料检索
    /** 按「课程 + 关键条款」检索支撑资料；检索不足就不硬凑 */
    private List<RetrievalService.Hit> retrieveEvidence(Long courseId, String courseName) {
        if (courseId == null) {
            return List.of();
        }
        StringBuilder q = new StringBuilder();
        q.append(nz(courseName)).append(" 考核方式 成绩构成 及格线");
        try {
            RetrievalService.Outcome out = retrievalService.retrieve(courseId, q.toString().trim(), 3);
            RetrievalService.Guard guard = retrievalService.guard(out, q.toString());
            if (guard.refuse()) {
                return List.of();
            }
            return out.hits();
        } catch (Exception e) {
            log.warn("[GroupReport] 资料检索失败，报告照常生成：{}", e.getMessage());
            return List.of();
        }
    }

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

    /** 末尾补「参考依据」段落（AI 与模板两条路都带） */
    private void appendEvidence(ReportContent content, List<RetrievalService.Hit> evidence) {
        if (content == null) {
            return;
        }
        String text;
        if (evidence == null || evidence.isEmpty()) {
            text = "本课程的课程资料库里暂未检索到与本次分析直接对应的条款。";
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
        content.sections.add(new ReportContent.Section("参考依据", text));
    }
    /** 报告内容内部结构（段落 + 动作） */
    static class ReportContent {
        private String summary;
        private final List<Section> sections;
        private final List<Map<String, Object>> actions = new ArrayList<>();

        ReportContent(String summary, List<Section> sections) {
            this.summary = summary;
            this.sections = sections;
        }

        String getSummary() {
            return summary;
        }

        Map<String, Object> toSections() {
            Map<String, Object> m = new LinkedHashMap<>();
            List<Map<String, Object>> list = new ArrayList<>();
            for (Section s : sections) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", s.title);
                item.put("text", s.text);
                list.add(item);
            }
            m.put("summary", summary);
            m.put("sections", list);
            m.put("actions", actions);
            return m;
        }

        static class Section {
            private final String title;
            private final String text;

            Section(String title, String text) {
                this.title = title;
                this.text = text;
            }

            String getTitle() {
                return title;
            }

            String getText() {
                return text;
            }
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

    private static String nz(String s) {
        return s == null ? "-" : s;
    }

    private static String nzf(Integer i) {
        return i == null ? "-" : String.valueOf(i);
    }

    private static int nzfInt(Integer i) {
        return i == null ? 0 : i;
    }

    private static String num(BigDecimal v) {
        if (v == null) {
            return "-";
        }
        return v.setScale(1, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String pct(BigDecimal rate) {
        if (rate == null) {
            return "-";
        }
        return rate.multiply(BigDecimal.valueOf(100)).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
