package com.example.academic_risk_warning.controller.student;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.AlertEventService;
import com.example.academic_risk_warning.service.StudentWarningService;
import com.example.academic_risk_warning.vo.AlertRowVO;
import com.example.academic_risk_warning.vo.StudentWarningVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 学生端 · 我的学情预警（第二阶段第 4 步）。
 *
 * <p>只读结论 + 申诉入口：一门课一条结论，只有结论与建议，不含教师内部处置记录；
 * 申诉写到 alert_record.student_response 并通知任课教师与辅导员。</p>
 */
@RestController
@RequestMapping("/api/student/warnings")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentWarningController {

    private final StudentWarningService studentWarningService;
    private final AlertEventService alertEventService;
    private final JdbcTemplate jdbc;

    /** 概览：每门课一条结论 + 建议 + 判定口径（termId 不传为当前学期） */
    @GetMapping
    public Result<StudentWarningVO> overview(@RequestParam(required = false) Long termId) {
        return Result.ok(studentWarningService.overview(UserContext.require(), termId));
    }

    /** 学生预警首页的 Agent 真实状态、报告趋势和等级分布 */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Long studentId = UserContext.require().getRefId();
        StudentWarningVO ov = studentWarningService.overview(UserContext.require(), null);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("summary", ov.getSummary());
        out.put("items", ov.getItems());
        out.put("termName", ov.getTermName());

        List<Map<String, Object>> reports = jdbc.queryForList("""
                select r.id, c.course_name, r.risk_level, r.total_score, r.source, r.stale, r.create_time, r.run_id
                from ai_report r join course c on c.id = r.course_id
                where r.student_id=? and r.scope='STUDENT' order by r.id desc limit 12
                """, studentId);
        Map<String, Object> latest = reports.isEmpty() ? null : reports.get(0);
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = reports.size() - 1; i >= 0; i--) {
            Map<String, Object> r = reports.get(i);
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", r.get("id"));
            p.put("courseName", r.get("course_name"));
            p.put("riskLevel", r.get("risk_level"));
            p.put("score", r.get("total_score"));
            p.put("time", r.get("create_time"));
            trend.add(p);
        }
        out.put("trend", trend);
        out.put("latestReport", latest);

        Map<String, Object> s = ov.getSummary() == null ? Map.of() : Map.of(
                "red", ov.getSummary().getRed(), "orange", ov.getSummary().getOrange(),
                "yellow", ov.getSummary().getYellow(), "green", ov.getSummary().getGreen());
        out.put("distribution", s);

        List<Map<String, Object>> agent = new ArrayList<>();
        agent.add(agentCard("数据体检", latest != null ? "READY" : "未运行", latest != null));
        agent.add(agentCard("数据分析", (ov.getItems() == null ? 0 : ov.getItems().size()) + " 门课", ov.getItems() != null));
        int risk = ov.getSummary() == null ? 0 : ov.getSummary().getRed() + ov.getSummary().getOrange() + ov.getSummary().getYellow();
        agent.add(agentCard("学情研判", risk + " 门风险", true));
        agent.add(agentCard("RAG 证据", latest != null ? "已附报告" : "未运行", latest != null));
        agent.add(agentCard("干预建议", ov.getAdvice() == null || ov.getAdvice().isEmpty() ? "未生成" : ov.getAdvice().size() + " 条建议", ov.getAdvice() != null && !ov.getAdvice().isEmpty()));
        agent.add(agentCard("报告生成", latest == null ? "未生成" : String.valueOf(latest.get("source")), latest != null));
        agent.add(agentCard("质量校验", latest == null ? "未运行" : (Boolean.TRUE.equals(latest.get("stale")) ? "需复核" : "PASS"), latest != null));
        out.put("agent", agent);
        return Result.ok(out);
    }

    private static Map<String, Object> agentCard(String name, String status, boolean ok) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("status", status);
        m.put("ok", ok);
        return m;
    }

    /** 单课详情：结论 + 学生可见时间线 + 建议 + 口径 */
    @GetMapping("/{enrollmentId}")
    public Result<StudentWarningVO.Detail> detail(@PathVariable Long enrollmentId) {
        return Result.ok(studentWarningService.detail(UserContext.require(), enrollmentId));
    }

    /** 提交申诉（每门课只能申诉一次） */
    @PostMapping("/{alertId}/appeal")
    public Result<AlertRowVO> appeal(@PathVariable Long alertId, @RequestBody AppealRequest request) {
        return Result.ok(alertEventService.studentAppeal(UserContext.require(), alertId,
                request == null ? null : request.getReason()));
    }

    /** 申诉请求体 */
    @lombok.Data
    public static class AppealRequest {
        private String reason;
    }
}
