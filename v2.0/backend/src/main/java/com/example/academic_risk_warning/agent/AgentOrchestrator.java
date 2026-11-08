package com.example.academic_risk_warning.agent;

import com.example.academic_risk_warning.agent.exec.ToolCallExecutor;
import com.example.academic_risk_warning.agent.tool.ToolContext;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * P2-3 极简 AgentOrchestrator：路由 → 计划 → 受限 JSON 执行（步数预算）。
 *
 * <p>本阶段只做确定性工具链（Planner/Executor 合一），为 P2-4 四角色与 P2-6 台账提供骨架；
 * 同题多次执行结果一致（无随机、无模型调用）。</p>
 */
@Component
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final IntentRouter router;
    private final ToolCallExecutor executor;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    public record Step(int stepNo, String tool, Map<String, Object> args, boolean ok, String errorCode,
                       long elapsedMs, String digest, Map<String, Object> evidence) {
    }

    public record RunResult(String question, String intent, String intentLabel, String routeMethod,
                            double confidence, int maxSteps, int usedSteps, boolean blocked,
                            String blockedReason, List<Step> steps, String answerDraft, long elapsedMs) {
    }

    private record Plan(String tool, Map<String, Object> args) {
    }

    public RunResult run(String question, ToolContext ctx, Integer maxStepsArg) {
        long t0 = System.currentTimeMillis();
        int maxSteps = maxStepsArg == null ? 4 : Math.max(1, Math.min(8, maxStepsArg));
        IntentRouter.RouteResult route = router.route(question);
        List<Plan> plans = plan(question, route.intent(), ctx);
        List<Step> steps = new ArrayList<>();
        boolean blocked = false;
        String blockedReason = null;
        int used = 0;
        for (Plan plan : plans) {
            if (used >= maxSteps) {
                blocked = true;
                blockedReason = "步数预算已用尽（" + maxSteps + "）";
                break;
            }
            String raw;
            try {
                Map<String, Object> call = new LinkedHashMap<>();
                call.put("tool", plan.tool());
                call.put("args", plan.args());
                raw = json.writeValueAsString(call);
            } catch (Exception e) {
                blocked = true;
                blockedReason = "工具调用序列化失败：" + e.getMessage();
                break;
            }
            ToolCallExecutor.Outcome out = executor.execute(raw, ctx, used, maxSteps);
            used = out.usedStepsAfter();
            steps.add(new Step(steps.size() + 1, out.tool(), out.args(), out.ok(),
                    out.result().getErrorCode(), out.result().getElapsedMs(), out.result().getDigest(),
                    out.result().getEvidence()));
            if (!out.ok()) {
                blocked = true;
                blockedReason = out.result().getErrorCode() + "：" + out.result().getMessage();
                break;
            }
        }
        return new RunResult(question, route.intent().name(), route.intent().label(), route.method(),
                route.confidence(), maxSteps, used, blocked, blockedReason, steps,
                draft(route.intent(), steps, blocked, blockedReason),
                System.currentTimeMillis() - t0);
    }

    private List<Plan> plan(String question, IntentRouter.Intent intent, ToolContext ctx) {
        List<Plan> plans = new ArrayList<>();
        switch (intent) {
            case CLASS_STATS -> plans.add(new Plan("getClassRiskStats", Map.of()));
            case TREND -> plans.add(new Plan("getRiskTrend", Map.of()));
            case COURSE_COMPARE -> plans.add(new Plan("getCourseCompare", Map.of("topN", 10)));
            case STUDENT_RISK -> plans.add(new Plan("getStudentRisk", Map.of("studentId", studentId(question))));
            case INTERVENTION -> {
                plans.add(new Plan("getInterventions", Map.of("studentId", studentId(question))));
                plans.add(new Plan("getClassRiskStats", Map.of()));
            }
            case COURSE_QA -> plans.add(new Plan("retrieveCourseDocs",
                    Map.of("courseId", courseId(question), "query", question, "topK", 5)));
            default -> plans.add(new Plan("getClassRiskStats", Map.of()));
        }
        return plans;
    }

    private long studentId(String question) {
        Matcher m = Pattern.compile("(?:学生|学号|student)\\s*[:：]?\\s*(\\d{1,8})").matcher(question);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }
        m = Pattern.compile("\\b(\\d{4,8})\\b").matcher(question);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }
        return 1001L;
    }

    private long courseId(String question) {
        Matcher m = Pattern.compile("(?:课程|course)\\s*[:：]?\\s*(\\d{1,4})").matcher(question);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }
        for (Map<String, Object> row : jdbc.queryForList("select id, course_name from course")) {
            String name = String.valueOf(row.get("course_name"));
            if (!name.isEmpty() && question.contains(name)) {
                return ((Number) row.get("id")).longValue();
            }
        }
        return 4L;
    }

    private String draft(IntentRouter.Intent intent, List<Step> steps, boolean blocked, String reason) {
        StringBuilder sb = new StringBuilder();
        sb.append("意图：").append(intent.label()).append("；");
        sb.append("已执行 ").append(steps.size()).append(" 个工具。");
        if (!steps.isEmpty()) {
            Step last = steps.get(steps.size() - 1);
            sb.append("关键证据：").append(evidenceText(last.evidence()));
        }
        if (blocked) {
            sb.append("（已阻断：").append(reason).append("）");
        }
        return sb.toString();
    }

    private String evidenceText(Map<String, Object> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return "无";
        }
        StringBuilder sb = new StringBuilder();
        evidence.forEach((k, v) -> {
            if (sb.length() > 0) sb.append(", ");
            sb.append(k).append('=').append(v);
        });
        return sb.length() > 220 ? sb.substring(0, 219) + "…" : sb.toString();
    }
}
