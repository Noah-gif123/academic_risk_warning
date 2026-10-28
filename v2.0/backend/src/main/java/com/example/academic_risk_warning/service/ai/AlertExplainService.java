package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.config.AiProperties;
import com.example.academic_risk_warning.vo.ai.AlertExplainVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

/**
 * 预警解释卡 + 干预建议包（M9）。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>**先规则后语言**：触发类型（缺勤/作业/项目/分数/多门/数据不足）由规则判定，
 *       对应三条例行动作由规则库给出；模型只负责把"为什么—差多少—怎么补"写成人话；</li>
 *   <li>**数字可复算**：事实包写入 `fact_json`，并对模型输出做白名单回验；</li>
 *   <li>**三层通道**：Dify 解释卡工作流 → 直连模型 → 规则模板，永远有结果。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertExplainService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbc;
    private final ReportComposer composer;
    private final DifyClient difyClient;
    private final DashScopeClient dashScopeClient;
    private final AiRunService runService;
    private final AiProperties props;

    private static final Map<String, String> TYPE_TEXT = Map.of(
            "ABSENCE", "缺勤超限",
            "HOMEWORK", "作业欠交",
            "PROJECT", "项目掉队",
            "SCORE", "分数临界",
            "MULTI", "多门联动",
            "DATA", "数据不足");

    // ============================================================ 生成
    public AlertExplainVO generate(Long alertId, Long operatorId) {
        Map<String, Object> fact = buildFact(alertId);
        String triggerType = String.valueOf(fact.get("triggerType"));
        List<Map<String, Object>> actions = actionsFor(triggerType, fact);

        Long runId = runService.start("ALERT_EXPLAIN", "ALERT", alertId, operatorId);
        long t0 = System.currentTimeMillis();
        List<Content.Section> sections = null;
        String source = "TEMPLATE";
        String status = "FALLBACK";
        StringBuilder note = new StringBuilder();
        int tokens = 0;

        String systemPrompt = systemPrompt();
        String userPrompt = userPrompt(fact, actions);

        if (props.isEnabled() && props.getAlertExplainAppKey() != null && !props.getAlertExplainAppKey().isBlank()) {
            Map<String, Object> inputs = new LinkedHashMap<>();
            inputs.put("facts", userPrompt);
            inputs.put("trigger_type", triggerType);
            DifyClient.WorkflowResult res = difyClient.runWorkflow(props.getAlertExplainAppKey(), inputs, "alert-" + alertId);
            tokens += res.getTotalTokens();
            if (res.isOk()) {
                String text = pickText(res.getOutputs());
                List<String> bad = composer.invalidNumbersAgainst(text, fact);
                runService.step(runId, 1, "dify-alert-workflow", "alert-explain", text,
                        bad.isEmpty() ? "PASS" : "WARN",
                        bad.isEmpty() ? null : "越界数字 " + bad.stream().distinct().limit(6).toList(),
                        System.currentTimeMillis() - t0);
                if (bad.isEmpty() && composer.looksLikeSections(text, 2)) {
                    sections = mapSections(composer.parseAnySections(text));
                    source = "DIFY";
                    status = "SUCCESS";
                } else {
                    note.append("Dify 输出不可用；");
                }
            } else {
                note.append("Dify 失败：").append(res.getError()).append("；");
            }
        }

        if (sections == null && props.directReady()) {
            long t1 = System.currentTimeMillis();
            DashScopeClient.ChatResult res = dashScopeClient.chat(systemPrompt, userPrompt);
            tokens += res.getTotalTokens();
            if (res.isOk()) {
                List<String> bad = composer.invalidNumbersAgainst(res.getText(), fact);
                runService.step(runId, 2, "direct-chat", "alert-explain", res.getText(),
                        bad.isEmpty() ? "PASS" : "WARN",
                        bad.isEmpty() ? null : "越界数字 " + bad.stream().distinct().limit(6).toList(),
                        System.currentTimeMillis() - t1);
                if (bad.isEmpty() && composer.looksLikeSections(res.getText(), 2)) {
                    sections = mapSections(composer.parseAnySections(res.getText()));
                    source = "LLM";
                    status = "SUCCESS";
                } else {
                    note.append("直连输出不可用；");
                }
            } else {
                note.append("直连失败：").append(res.getError()).append("；");
            }
        }

        if (sections == null) {
            sections = templateSections(fact, actions);
            note.append("已使用规则模板");
        }

        String summary = sections.isEmpty() ? "" : sections.get(0).getText();
        Long id = persist(fact, triggerType, sections, actions, summary, source, runId, tokens, operatorId);
        runService.usage(runId, "ALERT_EXPLAIN", tokens, operatorId);
        runService.finish(runId, status, source, tokens, status.equals("SUCCESS") ? null : note.toString());
        return detail(id);
    }

    // ============================================================ 事实与规则
    private Map<String, Object> buildFact(Long alertId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select ar.id, ar.student_id, ar.enrollment_id, ar.teaching_class_id, ar.course_id, ar.term_id,
                       ar.alert_level, ar.trigger_channel, ar.trigger_rule, ar.alert_reason, ar.evidence_json,
                       ar.risk_score, ar.confidence, ar.output_mode, ar.status,
                       s.student_no, s.student_name, c.course_name, tc.class_code, tc.class_name
                from alert_record ar
                         join student s on s.id = ar.student_id
                         left join course c on c.id = ar.course_id
                         left join teaching_class tc on tc.id = ar.teaching_class_id
                where ar.id = ? and ar.is_void = 0
                """, alertId);
        if (rows.isEmpty()) {
            throw new BizException("预警事件不存在：" + alertId);
        }
        Map<String, Object> a = rows.get(0);
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("alertId", alertId);
        f.put("alertLevel", str(a.get("alert_level")));
        f.put("triggerChannel", str(a.get("trigger_channel")));
        f.put("triggerRule", str(a.get("trigger_rule")));
        f.put("alertReason", str(a.get("alert_reason")));
        f.put("studentName", str(a.get("student_name")));
        f.put("studentNo", str(a.get("student_no")));
        f.put("courseName", str(a.get("course_name")));
        f.put("className", str(a.get("class_name")) != null ? str(a.get("class_name")) : str(a.get("class_code")));
        f.put("confidence", a.get("confidence"));
        f.put("outputMode", str(a.get("output_mode")));

        Long enrollmentId = lng(a.get("enrollment_id"));
        Long studentId = lng(a.get("student_id"));
        Long classId = lng(a.get("teaching_class_id"));
        Long courseId = lng(a.get("course_id"));
        Long termId = lng(a.get("term_id"));
        f.put("enrollmentId", enrollmentId);
        f.put("studentId", studentId);
        f.put("teachingClassId", classId);
        f.put("courseId", courseId);
        f.put("termId", termId);

        // 判定结果
        List<Map<String, Object>> rs = jdbc.queryForList("""
                select absent_count, late_count, leave_count, allowed_absent, remaining_absent,
                       hard_fail, near_fail, final_exam_needed, final_unreachable, risk_score,
                       confidence_level, output_mode, data_quality_json
                from risk_state where enrollment_id = ? and is_void = 0 order by id desc limit 1
                """, enrollmentId);
        if (!rs.isEmpty()) {
            Map<String, Object> r = rs.get(0);
            f.put("absent", i(r.get("absent_count")));
            f.put("late", i(r.get("late_count")));
            f.put("leave", i(r.get("leave_count")));
            f.put("allowedAbsent", i(r.get("allowed_absent")));
            f.put("remainingAbsent", i(r.get("remaining_absent")));
            f.put("hardFail", b(r.get("hard_fail")));
            f.put("nearFail", b(r.get("near_fail")));
            f.put("finalExamNeeded", r.get("final_exam_needed"));
            f.put("confidenceLevel", str(r.get("confidence_level")));
            if (f.get("outputMode") == null) {
                f.put("outputMode", str(r.get("output_mode")));
            }
        }

        // 成绩与班级对比
        List<Map<String, Object>> sr = jdbc.queryForList("""
                select total_score, weighted_score, deduction_points, fail_reason
                from score_result where enrollment_id = ? and is_void = 0 order by id desc limit 1
                """, enrollmentId);
        if (!sr.isEmpty()) {
            Map<String, Object> r = sr.get(0);
            f.put("totalScore", r.get("total_score"));
            f.put("weightedScore", r.get("weighted_score"));
            f.put("deductionPoints", r.get("deduction_points"));
            f.put("failReason", str(r.get("fail_reason")));
        }
        if (classId != null) {
            List<Map<String, Object>> agg = jdbc.queryForList("""
                    select avg(sr.total_score) as avg_score, count(*) as size
                    from score_result sr join enrollment e on e.id = sr.enrollment_id
                    where e.teaching_class_id = ? and sr.is_void = 0 and sr.total_score is not null
                    """, classId);
            if (!agg.isEmpty() && agg.get(0).get("avg_score") != null) {
                f.put("classAvg", dec(agg.get(0).get("avg_score")));
                f.put("classSize", i(agg.get(0).get("size")));
            }
            Object mine = f.get("totalScore");
            if (mine != null) {
                Integer better = jdbc.queryForObject("""
                        select count(*) from score_result sr join enrollment e on e.id = sr.enrollment_id
                        where e.teaching_class_id = ? and sr.is_void = 0 and sr.total_score > ?
                        """, Integer.class, classId, new BigDecimal(String.valueOf(mine)));
                f.put("classRank", (better == null ? 0 : better) + 1);
            }
        }

        // 欠交作业 / 项目参与
        Integer expectedHomework = jdbc.queryForObject("""
                select count(*) from paper p
                where p.teaching_class_id = ? and p.status <> 'DRAFT' and p.paper_type in ('QUIZ','HOMEWORK')
                """, Integer.class, classId);
        Integer submitted = jdbc.queryForObject("""
                select count(*) from submission where enrollment_id = ? and submit_status in ('SUBMITTED','MARKED')
                """, Integer.class, enrollmentId);
        int exp = expectedHomework == null ? 0 : expectedHomework;
        int sub = submitted == null ? 0 : submitted;
        f.put("homeworkExpected", exp);
        f.put("homeworkSubmitted", sub);
        f.put("homeworkMissing", Math.max(0, exp - sub));

        Integer projectExpected = jdbc.queryForObject("""
                select count(*) from project_task where offering_id = ? and status <> 'DRAFT'
                """, Integer.class, f.get("courseId") == null ? -1L : courseId);
        Integer projectDone = jdbc.queryForObject("""
                select count(*) from project_submission ps join project_task pt on pt.id = ps.project_id
                where ps.student_id = ? and pt.offering_id = ?
                """, Integer.class, studentId, f.get("courseId") == null ? -1L : courseId);
        int pexp = projectExpected == null ? 0 : projectExpected;
        int pdone = projectDone == null ? 0 : projectDone;
        f.put("projectExpected", pexp);
        f.put("projectDone", pdone);
        f.put("projectMissing", Math.max(0, pexp - pdone));

        // 多门预警（当前学期）
        Integer multiCourse = jdbc.queryForObject("""
                select count(*) from risk_state rs join enrollment e on e.id = rs.enrollment_id
                join term t on t.id = e.term_id and t.is_current = 1
                where rs.student_id = ? and rs.is_void = 0 and rs.risk_level in ('RED','YELLOW')
                """, Integer.class, studentId);
        f.put("riskCourseCount", multiCourse == null ? 0 : multiCourse);

        // 薄弱分组（本课程，最多 3 个）
        List<Map<String, Object>> weak = jdbc.queryForList("""
                select coalesce(q.group_name,'未分组') as name, sum(sa.score) / nullif(sum(q.full_score),0) as rate
                from submission_answer sa
                         join submission s on s.id = sa.submission_id
                         join question q on q.id = sa.question_id
                where s.student_id = ? and s.teaching_class_id = ?
                group by coalesce(q.group_name,'未分组')
                having sum(q.full_score) > 0
                order by rate asc limit 3
                """, studentId, classId);
        List<Map<String, Object>> weakList = new ArrayList<>();
        for (Map<String, Object> w : weak) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", str(w.get("name")));
            item.put("rate", dec(w.get("rate")));
            weakList.add(item);
        }
        f.put("weakGroups", weakList);

        // 触发类型判定（规则优先）
        String type = classify(f);
        f.put("triggerType", type);
        f.put("triggerTypeText", TYPE_TEXT.getOrDefault(type, "综合风险"));
        f.put("totalScoreText", num(f.get("totalScore")));
        f.put("classAvgText", num(f.get("classAvg")));
        f.put("finalNeedText", num(f.get("finalExamNeeded")));
        return f;
    }

    private String classify(Map<String, Object> f) {
        Integer absent = i(f.get("absent"));
        Integer allowed = i(f.get("allowedAbsent"));
        boolean hard = Boolean.TRUE.equals(f.get("hardFail"));
        Integer remaining = i(f.get("remainingAbsent"));
        boolean usedUp = remaining != null && remaining <= 0 && absent != null && allowed != null && absent >= allowed;
        if (hard || usedUp || (absent != null && allowed != null && absent > allowed)) {
            return "ABSENCE";
        }
        String mode = str(f.get("outputMode"));
        Object conf = f.get("confidence");
        double confVal = conf instanceof Number n ? n.doubleValue() : 1d;
        if ("BEHAVIOR_ONLY".equals(mode) || confVal < 0.15) {
            return "DATA";
        }
        if (i(f.get("homeworkMissing")) != null && i(f.get("homeworkMissing")) >= 2) {
            return "HOMEWORK";
        }
        if (i(f.get("projectMissing")) != null && i(f.get("projectMissing")) >= 1) {
            return "PROJECT";
        }
        if (i(f.get("riskCourseCount")) != null && i(f.get("riskCourseCount")) >= 3) {
            return "MULTI";
        }
        return "SCORE";
    }

    /** 干预建议包：每类三条例行动作（规则库；模型只做表述） */
    private List<Map<String, Object>> actionsFor(String type, Map<String, Object> f) {
        List<Map<String, Object>> list = new ArrayList<>();
        switch (type) {
            case "ABSENCE" -> {
                list.add(action("ABSENCE_TALK", "找任课老师说明缺勤原因",
                        Map.of("teachingClassId", f.get("teachingClassId"), "enrollmentId", f.get("enrollmentId"))));
                list.add(action("ABSENCE_RECORD", "核对考勤记录（缺勤 " + i(f.get("absent")) + " 次 / 允许 " + i(f.get("allowedAbsent")) + " 次）",
                        Map.of("enrollmentId", f.get("enrollmentId"))));
                list.add(action("ABSENCE_FULL", "剩余课时全勤，把缺勤控制在当前次数",
                        Map.of("enrollmentId", f.get("enrollmentId"))));
            }
            case "HOMEWORK" -> {
                list.add(action("HOMEWORK_FILL", "补齐 " + i(f.get("homeworkMissing")) + " 次未交作业",
                        Map.of("teachingClassId", f.get("teachingClassId"), "enrollmentId", f.get("enrollmentId"))));
                list.add(action("HOMEWORK_REDO", "把最近两次作业的错题重做一遍",
                        Map.of("teachingClassId", f.get("teachingClassId"))));
                list.add(action("HOMEWORK_ROUTINE", "每周固定两次作业时间，提交前自查一遍",
                        Map.of()));
            }
            case "PROJECT" -> {
                list.add(action("PROJECT_ASK", "找组长/队友确认项目分工与当前进度",
                        Map.of("offeringId", f.get("courseId"))));
                list.add(action("PROJECT_SUBMIT", "查看项目要求并补交 " + i(f.get("projectMissing")) + " 个项目",
                        Map.of("offeringId", f.get("courseId"))));
                list.add(action("PROJECT_PLAN", "把大作业拆成两周小目标，逐项打卡",
                        Map.of()));
            }
            case "SCORE" -> {
                String weak = firstWeak(f);
                list.add(action("SCORE_WEAK", weak == null ? "按失分最多的板块复习" : "重点补「" + weak + "」这一块",
                        Map.of("teachingClassId", f.get("teachingClassId"))));
                list.add(action("SCORE_REVIEW", "看本课程导学与课件对应章节",
                        Map.of("offeringId", f.get("courseId"), "teachingClassId", f.get("teachingClassId"))));
                list.add(action("SCORE_ASK", "找任课老师答疑，把不懂的题问清楚",
                        Map.of("teachingClassId", f.get("teachingClassId"))));
            }
            case "MULTI" -> {
                list.add(action("MULTI_PRIORITY", "先保住风险最高的一门课（当前 " + i(f.get("riskCourseCount")) + " 门有预警）",
                        Map.of("studentId", f.get("studentId"))));
                list.add(action("MULTI_PLAN", "和辅导员一起制定周计划，每周复盘一次",
                        Map.of("studentId", f.get("studentId"))));
                list.add(action("MULTI_TALK", "找辅导员登记干预记录，便于后续跟踪",
                        Map.of("studentId", f.get("studentId"))));
            }
            default -> {
                list.add(action("DATA_CHECK", "和任课老师核对考勤/成绩是否已登全",
                        Map.of("teachingClassId", f.get("teachingClassId"))));
                list.add(action("DATA_WAIT", "等成绩补齐后再看一次结论", Map.of()));
                list.add(action("DATA_ASK", "如有疑问可向老师提出申诉", Map.of("enrollmentId", f.get("enrollmentId"))));
            }
        }
        return list;
    }

    private Map<String, Object> action(String type, String label, Map<String, Object> payload) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("label", label);
        m.put("payload", payload == null ? Map.of() : payload);
        return m;
    }

    private String firstWeak(Map<String, Object> f) {
        Object w = f.get("weakGroups");
        if (w instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> m) {
            return m.get("name") == null ? null : String.valueOf(m.get("name"));
        }
        return null;
    }

    // ============================================================ 提示词与模板
    private String systemPrompt() {
        return """
                你是高校学业预警系统里的「预警解释助手」，服务对象是学生本人与任课教师。
                必须遵守：
                1. 只能使用给定事实里的数字，禁止自己计算、换算或编造任何数字；
                2. 严格输出四段，标题只能是【为什么会预警】【你现在差多少】【马上能做的三件事】【数据与口径说明】，标题独占一行；
                3. 第二人称、语气平实，不出现"挂科""必挂""肯定过不了"等判定词，风险结论只引用给定的预警等级；
                4. 第三段必须逐条使用给定的三条动作，不要自己新增动作；
                5. 不使用 Markdown 星号、表格、代码块；总字数 300 到 450 字；
                6. 直接输出正文，不要输出思考过程、不要复述规则。
                """;
    }

    private String userPrompt(Map<String, Object> f, List<Map<String, Object>> actions) {
        StringBuilder sb = new StringBuilder();
        sb.append("课程：").append(str(f.get("courseName"))).append("，班级：").append(str(f.get("className")))
                .append("，学生：").append(str(f.get("studentName"))).append("\n");
        sb.append("预警等级：").append(alertText(str(f.get("alertLevel"))))
                .append("；触发规则：").append(nz(str(f.get("triggerRule"))))
                .append("；触发类型：").append(str(f.get("triggerTypeText"))).append("\n");
        sb.append("预警原因（系统原文）：").append(nz(str(f.get("alertReason")))).append("\n");
        sb.append("考勤：缺勤 ").append(nzf(i(f.get("absent")))).append(" 次、迟到 ").append(nzf(i(f.get("late"))))
                .append(" 次、请假 ").append(nzf(i(f.get("leave"))))
                .append(" 次；允许缺勤 ").append(nzf(i(f.get("allowedAbsent"))))
                .append(" 次，剩余额度 ").append(nzf(i(f.get("remainingAbsent")))).append(" 次\n");
        sb.append("成绩：当前总分 ").append(num(f.get("totalScore")))
                .append("，班级均分 ").append(num(f.get("classAvg")))
                .append("，班内排名 ").append(nzf(i(f.get("classRank")))).append(" / ").append(nzf(i(f.get("classSize"))))
                .append("，扣分 ").append(num(f.get("deductionPoints"))).append("\n");
        sb.append("差距：期末需考 ").append(num(f.get("finalExamNeeded"))).append("；置信度 ")
                .append(confText(str(f.get("confidenceLevel")))).append("；输出模式 ")
                .append(nz(str(f.get("outputMode")))).append("\n");
        sb.append("欠交作业 ").append(nzf(i(f.get("homeworkMissing"))))
                .append(" 次（应交 ").append(nzf(i(f.get("homeworkExpected"))))
                .append(" 次，已交 ").append(nzf(i(f.get("homeworkSubmitted")))).append(" 次）\n");
        sb.append("项目：应交 ").append(nzf(i(f.get("projectExpected"))))
                .append(" 个，已提交 ").append(nzf(i(f.get("projectDone")))).append(" 个\n");
        sb.append("当前学期有 ").append(nzf(i(f.get("riskCourseCount")))).append(" 门课处于红/黄灯\n");
        Object weak = f.get("weakGroups");
        if (weak instanceof List<?> list && !list.isEmpty()) {
            sb.append("薄弱分组：");
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    sb.append(m.get("name")).append("；");
                }
            }
            sb.append("\n");
        }
        sb.append("\n必须使用的三条动作：");
        int i = 1;
        for (Map<String, Object> a : actions) {
            sb.append("(").append(i++).append(") ").append(a.get("label")).append("；");
        }
        sb.append("\n请据此写这张预警解释卡。");
        return sb.toString();
    }

    private List<Content.Section> templateSections(Map<String, Object> f, List<Map<String, Object>> actions) {
        List<Content.Section> out = new ArrayList<>();
        String type = String.valueOf(f.get("triggerType"));
        String why = switch (type) {
            case "ABSENCE" -> "系统判定" + alertText(str(f.get("alertLevel"))) + "，直接原因是缺勤 " + nzf(i(f.get("absent")))
                    + " 次，已超过本课程允许的 " + nzf(i(f.get("allowedAbsent"))) + " 次上限。";
            case "HOMEWORK" -> "系统判定" + alertText(str(f.get("alertLevel"))) + "，主要原因是作业欠交 " + nzf(i(f.get("homeworkMissing")))
                    + " 次，平时分被拉低。";
            case "PROJECT" -> "系统判定" + alertText(str(f.get("alertLevel"))) + "，主要原因是还有 " + nzf(i(f.get("projectMissing")))
                    + " 个项目没有提交记录。";
            case "MULTI" -> "系统判定" + alertText(str(f.get("alertLevel"))) + "，当前学期共有 " + nzf(i(f.get("riskCourseCount")))
                    + " 门课同时处于红/黄灯，属于多门联动风险。";
            case "DATA" -> "系统判定" + alertText(str(f.get("alertLevel"))) + "，但目前成绩或考勤数据还不完整（置信度 "
                    + confText(str(f.get("confidenceLevel"))) + "），结论以行为提示为主。";
            default -> "系统判定" + alertText(str(f.get("alertLevel"))) + "，主要原因是当前总分 "
                    + num(f.get("totalScore")) + " 分，低于及格线附近。";
        };
        out.add(new Content.Section("为什么会预警", why + "触发规则：" + nz(str(f.get("triggerRule"))) + "。"));

        StringBuilder gap = new StringBuilder();
        gap.append("你当前总分 ").append(num(f.get("totalScore"))).append(" 分，班级均分 ")
                .append(num(f.get("classAvg"))).append(" 分，班内第 ").append(nzf(i(f.get("classRank"))))
                .append(" / ").append(nzf(i(f.get("classSize")))).append(" 位；");
        if (f.get("finalExamNeeded") != null) {
            gap.append("按当前平时分推算，期末需考 ").append(num(f.get("finalExamNeeded"))).append(" 分才能达标");
            if (i(f.get("classSize")) != null) {
                gap.append("；");
            }
        }
        gap.append("考勤缺口方面，剩余允许缺勤 ").append(nzf(i(f.get("remainingAbsent")))).append(" 次。");
        out.add(new Content.Section("你现在差多少", gap.toString()));

        StringBuilder acts = new StringBuilder();
        int idx = 1;
        for (Map<String, Object> a : actions) {
            acts.append(idx++).append("）").append(a.get("label")).append("；");
        }
        out.add(new Content.Section("马上能做的三件事", acts.toString()));

        out.add(new Content.Section("数据与口径说明",
                "本卡片的数字来自系统判定（判定口径：允许缺勤 = 有效课次 ÷ 3 向下取整；扣分 = 2×缺勤 + 1×迟到 + 1×max(0, 请假−3)）与成绩汇总，"
                        + "可逐项复算。若与实际情况不符，可在学情预警页面提交申诉，由任课教师核实。"));
        return out;
    }

    // ============================================================ 落库与读取
    private Long persist(Map<String, Object> fact, String triggerType, List<Content.Section> sections,
                         List<Map<String, Object>> actions, String summary, String source,
                         Long runId, int tokens, Long operatorId) {
        Map<String, Object> content = new LinkedHashMap<>();
        List<Map<String, Object>> secList = new ArrayList<>();
        for (Content.Section s : sections) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", s.title);
            item.put("text", s.text);
            secList.add(item);
        }
        content.put("summary", summary != null && summary.length() > 290 ? summary.substring(0, 289) + "…" : summary);
        content.put("sections", secList);
        content.put("actions", actions);

        String factJson;
        String contentJson;
        try {
            factJson = MAPPER.writeValueAsString(fact);
            contentJson = MAPPER.writeValueAsString(content);
        } catch (Exception e) {
            factJson = "{}";
            contentJson = "{}";
        }
        BigDecimal cost = BigDecimal.valueOf(props.costOf(tokens));
        String finalFact = factJson;
        String finalContent = contentJson;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    insert into ai_alert_explain(alert_id, enrollment_id, student_id, teaching_class_id, course_id, term_id,
                                                 alert_level, trigger_type, summary, fact_json, content_json, status, source,
                                                 model, app_id, prompt_version, run_id, tokens, cost, create_by)
                    values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setLong(i++, lng(fact.get("alertId")));
            ps.setLong(i++, lng(fact.get("enrollmentId")));
            ps.setLong(i++, lng(fact.get("studentId")));
            setNullableLong(ps, i++, lng(fact.get("teachingClassId")));
            setNullableLong(ps, i++, lng(fact.get("courseId")));
            setNullableLong(ps, i++, lng(fact.get("termId")));
            ps.setString(i++, str(fact.get("alertLevel")));
            ps.setString(i++, triggerType);
            ps.setString(i++, content.get("summary") == null ? null : String.valueOf(content.get("summary")));
            ps.setString(i++, finalFact);
            ps.setString(i++, finalContent);
            ps.setString(i++, "PUBLISHED");
            ps.setString(i++, source);
            ps.setString(i++, props.getModel());
            ps.setString(i++, props.getAlertExplainAppKey() == null || props.getAlertExplainAppKey().isBlank()
                    ? null : mask(props.getAlertExplainAppKey()));
            ps.setString(i++, "alert-explain-v1");
            setNullableLong(ps, i++, runId);
            ps.setInt(i++, tokens);
            ps.setBigDecimal(i++, cost);
            setNullableLong(ps, i++, operatorId);
            return ps;
        }, kh);
        return kh.getKey() == null ? null : kh.getKey().longValue();
    }

    public AlertExplainVO latest(Long alertId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select id from ai_alert_explain where alert_id = ? and status = 'PUBLISHED' order by id desc limit 1", alertId);
        if (rows.isEmpty()) {
            return null;
        }
        return detail(((Number) rows.get(0).get("id")).longValue());
    }

    public AlertExplainVO detail(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select e.id, e.alert_id, e.enrollment_id, e.student_id, e.alert_level, e.trigger_type,
                       e.summary, e.content_json, e.source, e.create_time,
                       s.student_name, c.course_name, tc.class_name
                from ai_alert_explain e
                         left join student s on s.id = e.student_id
                         left join course c on c.id = e.course_id
                         left join teaching_class tc on tc.id = e.teaching_class_id
                where e.id = ?
                """, id);
        if (rows.isEmpty()) {
            throw new BizException("解释卡不存在：" + id);
        }
        Map<String, Object> r = rows.get(0);
        AlertExplainVO vo = new AlertExplainVO();
        vo.setId(lng(r.get("id")));
        vo.setAlertId(lng(r.get("alert_id")));
        vo.setEnrollmentId(lng(r.get("enrollment_id")));
        vo.setAlertLevel(str(r.get("alert_level")));
        vo.setTriggerType(str(r.get("trigger_type")));
        vo.setTriggerTypeText(TYPE_TEXT.getOrDefault(str(r.get("trigger_type")), "综合风险"));
        vo.setSummary(str(r.get("summary")));
        vo.setCourseName(str(r.get("course_name")));
        vo.setClassName(str(r.get("class_name")));
        vo.setStudentName(str(r.get("student_name")));
        vo.setSource(str(r.get("source")));
        vo.setCreateTime(r.get("create_time") == null ? null : String.valueOf(r.get("create_time")));
        parseContent(str(r.get("content_json")), vo);
        return vo;
    }

    private void parseContent(String json, AlertExplainVO vo) {
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
                        AlertExplainVO.Section s = new AlertExplainVO.Section();
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
            log.warn("[AlertExplain] content 解析失败: {}", e.getMessage());
        }
    }

    /** 供权限校验：这条预警属于哪个学生 */
    public Long alertStudentId(Long alertId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select student_id from alert_record where id = ? and is_void = 0", alertId);
        if (rows.isEmpty()) {
            throw new BizException("预警事件不存在：" + alertId);
        }
        return lng(rows.get(0).get("student_id"));
    }

    /** 内容结构（段落） */
    static class Content {
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

    private List<Content.Section> mapSections(List<ReportComposer.GroupSection> groups) {
        List<Content.Section> out = new ArrayList<>();
        for (ReportComposer.GroupSection g : groups) {
            out.add(new Content.Section(g.getTitle(), g.getText()));
        }
        return out;
    }

    private String pickText(Map<String, Object> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return null;
        }
        for (String key : List.of("text", "result", "output", "answer")) {
            Object v = outputs.get(key);
            if (v != null && !String.valueOf(v).isBlank()) {
                return String.valueOf(v);
            }
        }
        return outputs.values().stream().filter(Objects::nonNull).map(String::valueOf)
                .filter(s -> s.length() > 40).findFirst().orElse(null);
    }

    // ============================================================ 工具
    private static String alertText(String level) {
        if ("RED".equals(level)) {
            return "红灯预警";
        }
        if ("YELLOW".equals(level)) {
            return "黄灯预警";
        }
        if ("GREEN".equals(level)) {
            return "绿灯（正常）";
        }
        return "风险提示";
    }

    private static String confText(String level) {
        if (level == null) {
            return "未知";
        }
        return switch (level) {
            case "HIGH" -> "高";
            case "MEDIUM" -> "中";
            case "LOW" -> "低";
            default -> level;
        };
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    private static String nzf(Integer i) {
        return i == null ? "-" : String.valueOf(i);
    }

    private static String num(Object v) {
        if (v == null) {
            return "-";
        }
        try {
            return new BigDecimal(String.valueOf(v)).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        } catch (Exception e) {
            return String.valueOf(v);
        }
    }

    private static void setNullableLong(PreparedStatement ps, int idx, Long v) throws java.sql.SQLException {
        if (v == null) {
            ps.setNull(idx, java.sql.Types.BIGINT);
        } else {
            ps.setLong(idx, v);
        }
    }

    private static String mask(String key) {
        if (key == null || key.length() <= 8) {
            return "app-****";
        }
        return key.substring(0, 7) + "****" + key.substring(key.length() - 3);
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Integer i(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Long lng(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Boolean b(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Boolean bb) {
            return bb;
        }
        if (v instanceof Number n) {
            return n.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(v));
    }

    private static BigDecimal dec(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        try {
            return new BigDecimal(String.valueOf(v)).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }
}
