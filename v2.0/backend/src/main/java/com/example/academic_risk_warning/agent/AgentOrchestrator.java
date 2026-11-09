package com.example.academic_risk_warning.agent;

import com.example.academic_risk_warning.agent.exec.ToolCallExecutor;
import com.example.academic_risk_warning.agent.ledger.EvidenceLedger;
import com.example.academic_risk_warning.agent.trace.AgentTraceService;
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
 * P2-3/4/5/6 编排器：路由 → 计划 → 受限 JSON 执行 → Critic → Writer + 台账。
 *
 * <p>角色：Planner / Executor / Critic / Writer。模式：
 * <ul>
 *   <li>mode=4：固定 4 角色；</li>
 *   <li>mode=2：固定 2 角色（Planner+Executor，Writer/校验仍内部执行）；</li>
 *   <li>mode=auto（默认）：简单意图 2 角色、复杂意图（干预/课程问答/多步）4 角色。</li>
 * </ul>
 * 单轮工具步数 ≤ maxSteps（默认 4）。Writer 的每个数字必须命中 P2-6 台账；
 * 未命中重写一次，仍失败则降级。</p>
 */
@Component
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final IntentRouter router;
    private final ToolCallExecutor executor;
    private final CriticRules criticRules;
    private final EvidenceLedger ledger;
    private final AgentTraceService trace;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    public record Step(int stepNo, String tool, Map<String, Object> args, boolean ok, String errorCode,
                       long elapsedMs, String digest, Map<String, Object> evidence,
                       Map<String, Object> data) {
    }

    public record RoleCall(String role, String detail, boolean ok) {
    }

    public record LedgerInfo(String runKey, int entries, int numbers, int hits, int misses,
                             double hitRate, boolean rewritten, boolean degraded, String finalDraft) {
    }

    public record RunResult(String question, String intent, String intentLabel, String routeMethod,
                            double confidence, String mode, int maxSteps, int usedSteps, boolean blocked,
                            String blockedReason, List<Step> steps, List<RoleCall> roles, int roleCalls,
                            LedgerInfo ledger, CriticRules.Result critic, Map<String, Object> quality,
                            String answerDraft, long elapsedMs, Long traceRunId) {
    }

    private record Plan(String tool, Map<String, Object> args) {
    }

    public RunResult run(String question, ToolContext ctx, Integer maxStepsArg) {
        return run(question, ctx, maxStepsArg, "auto");
    }

    public RunResult run(String question, ToolContext ctx, Integer maxStepsArg, String modeArg) {
        long t0 = System.currentTimeMillis();
        int maxSteps = maxStepsArg == null ? 4 : Math.max(1, Math.min(8, maxStepsArg));
        String mode = (modeArg == null || modeArg.isBlank()) ? "auto" : modeArg.trim().toLowerCase();
        IntentRouter.RouteResult route = router.route(question);
        List<Plan> plans = plan(question, route.intent(), ctx);
        boolean complex = route.intent() == IntentRouter.Intent.INTERVENTION
                || route.intent() == IntentRouter.Intent.COURSE_QA || plans.size() > 1;
        boolean fourRoles = "4".equals(mode) || ("auto".equals(mode) && complex);
        AgentTraceService.Start traceStart = trace.start("AGENT", question, ctx.user(), ctx.termId());
        String runKey = traceStart == null ? ledger.newRunKey() : traceStart.runKey();
        Long traceRunId = traceStart == null ? null : traceStart.runId();
        List<RoleCall> roles = new ArrayList<>();
        roles.add(new RoleCall("PLANNER", "plan=" + plans.stream().map(Plan::tool).toList(), true));

        List<Step> steps = new ArrayList<>();
        boolean blocked = false;
        String blockedReason = null;
        int used = 0;
        for (Plan plan : plans) {
            if (used >= maxSteps) {
                blocked = true;
                blockedReason = "STEP_BUDGET_EXCEEDED：步数预算 " + maxSteps;
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
                blockedReason = "SERIALIZE_ERROR：" + e.getMessage();
                break;
            }
            ToolCallExecutor.Outcome out = executor.execute(raw, ctx, used, maxSteps);
            used = out.usedStepsAfter();
            if (out.ok()) {
                ledger.record(runKey, steps.size() + 1, out.tool(), out.result().getEvidence());
            }
            steps.add(new Step(steps.size() + 1, out.tool(), out.args(), out.ok(),
                    out.result().getErrorCode(), out.result().getElapsedMs(), out.result().getDigest(),
                    out.result().getEvidence(), out.result().getData()));
            if (traceRunId != null) {
                try {
                    trace.step(traceRunId, steps.size(), "EXECUTOR", out.tool(),
                            json.writeValueAsString(plan.args()),
                            json.writeValueAsString(out.result().getEvidence()),
                            out.ok() ? "PASS" : "FAIL", out.result().getElapsedMs(),
                            out.result().getErrorCode());
                } catch (Exception ignored) {
                }
            }
            if (!out.ok()) {
                blocked = true;
                blockedReason = out.result().getErrorCode() + "：" + out.result().getMessage();
                break;
            }
        }
        roles.add(new RoleCall("EXECUTOR", "steps=" + steps.size() + ", used=" + used, !blocked));

        CriticRules.Result critic = null;
        if (fourRoles) {
            critic = criticRules.evaluate(route.intent().name(), toolRuns(steps));
            roles.add(new RoleCall("CRITIC", critic.message(), critic.passed()));
            if (!critic.passed() && !blocked) {
                blocked = true;
                blockedReason = "CRITIC_ASK_EVIDENCE：" + critic.message();
            }
        }

        String draft = blocked ? blockedDraft(blockedReason, critic) : buildDraft(steps);
        LedgerInfo ledgerInfo = applyLedger(runKey, draft);
        draft = ledgerInfo.finalDraft();
        if (!blocked && ledgerInfo.degraded()) {
            blocked = true;
            blockedReason = "LEDGER_DEGRADED：数字未完全入账";
        }
        if (fourRoles) {
            roles.add(new RoleCall("WRITER", "ledgerHit=" + ledgerInfo.hitRate(), !ledgerInfo.degraded()));
        }
        if (traceRunId != null) {
            try {
                if (critic != null) {
                    trace.step(traceRunId, 90, "CRITIC", null, null,
                            json.writeValueAsString(critic), critic.passed() ? "PASS" : "ASK_EVIDENCE", 0,
                            critic.message());
                }
                trace.step(traceRunId, 91, "WRITER", null, null,
                        json.writeValueAsString(ledgerInfo), ledgerInfo.degraded() ? "DEGRADED" : "PASS", 0,
                        ledgerInfo.runKey());
                trace.finish(traceRunId, blocked ? "DEGRADED" : "DONE", roles.size());
            } catch (Exception ignored) {
            }
        }
        Map<String, Object> quality = quality(route.intent().name(), steps, ledgerInfo);
        return new RunResult(question, route.intent().name(), route.intent().label(), route.method(),
                route.confidence(), mode, maxSteps, used, blocked, blockedReason, steps, roles,
                roles.size(), ledgerInfo, critic, quality, draft, System.currentTimeMillis() - t0, traceRunId);
    }

    private List<Plan> plan(String question, IntentRouter.Intent intent, ToolContext ctx) {
        List<Plan> plans = new ArrayList<>();
        switch (intent) {
            case CLASS_STATS -> {
                String className = className(question);
                plans.add(new Plan("getClassRiskStats",
                        className == null ? Map.of() : Map.of("className", className)));
            }
            case TREND -> plans.add(new Plan("getRiskTrend", Map.of()));
            case COURSE_COMPARE -> plans.add(new Plan("getCourseCompare", Map.of("topN", 10)));
            case STUDENT_RISK -> plans.add(new Plan("getStudentRisk", Map.of("studentId", studentId(question))));
            case INTERVENTION -> {
                plans.add(new Plan("getInterventions", Map.of("studentId", studentId(question))));
                plans.add(new Plan("getClassRiskStats", Map.of()));
            }
            case COURSE_QA -> plans.add(new Plan("retrieveCourseDocs",
                    Map.of("courseId", courseId(question), "query", question, "topK", 5, "mode", "FULLTEXT")));
            default -> plans.add(new Plan("getClassRiskStats", Map.of()));
        }
        return plans;
    }

    /** 从问题中提取“计算机1班”等班级名；没有则返回 null（保持全局口径）。 */
    private String className(String q) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("([\\u4e00-\\u9fa5A-Za-z]{1,12}[0-9]+班)").matcher(q == null ? "" : q);
        return m.find() ? m.group(1) : null;
    }

    private List<CriticRules.ToolRun> toolRuns(List<Step> steps) {
        List<CriticRules.ToolRun> out = new ArrayList<>();
        for (Step s : steps) {
            out.add(new CriticRules.ToolRun(s.tool(), s.args(), s.evidence()));
        }
        return out;
    }

    private String buildDraft(List<Step> steps) {
        Map<String, Object> merged = new LinkedHashMap<>();
        for (Step s : steps) {
            if (s.evidence() != null) {
                merged.putAll(s.evidence());
            }
        }
        StringBuilder sb = new StringBuilder("根据工具证据：");
        int n = 0;
        for (Map.Entry<String, Object> e : merged.entrySet()) {
            Object v = e.getValue();
            if (v == null || v instanceof Map || v instanceof List) {
                continue;
            }
            if (n++ > 0) {
                sb.append("，");
            }
            sb.append(e.getKey()).append('=').append(v);
        }
        return sb.toString();
    }

    private String blockedDraft(String reason, CriticRules.Result critic) {
        if (critic != null && critic.asksEvidence()) {
            return "证据不足，暂不能给出结论。需要补充：" +
                    (critic.missing().isEmpty() ? "可用工具证据" : String.join(",", critic.missing()));
        }
        return "本轮未能完成： " + (reason == null ? "未知原因" : reason);
    }

    private LedgerInfo applyLedger(String runKey, String draft) {
        EvidenceLedger.Repair repair = ledger.repair(runKey, draft);
        EvidenceLedger.Validation v = repair.second();
        return new LedgerInfo(runKey, v.entries(), v.numbers().size(), v.hits().size(), v.misses().size(),
                v.hitRate(), repair.rewritten(), repair.degraded(), repair.finalAnswer());
    }

    private Map<String, Object> quality(String intent, List<Step> steps, LedgerInfo li) {
        Map<String, Object> merged = new LinkedHashMap<>();
        for (Step s : steps) {
            if (s.evidence() != null) {
                merged.putAll(s.evidence());
            }
        }
        java.util.Set<String> required = criticRules.requiredKeys(intent);
        long present = required.stream().filter(merged::containsKey).count();
        double coverage = required.isEmpty() ? 1.0 : (double) present / required.size();
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("requiredCoverage", Math.round(coverage * 10000.0) / 10000.0);
        q.put("ledgerHitRate", li.hitRate());
        q.put("stepCount", steps.size());
        return q;
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
}
