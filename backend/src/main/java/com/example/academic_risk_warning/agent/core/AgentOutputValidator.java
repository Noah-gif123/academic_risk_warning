package com.example.academic_risk_warning.agent.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 智能体输出校验器（W2）。
 *
 * <p>三层校验：
 * <ol>
 *   <li><b>结构（schema）</b>：必需字段是否存在、是否为期望类型；</li>
 *   <li><b>取值</b>：等级/可行性等枚举值是否合法，百分比是否落在 0~100；</li>
 *   <li><b>真值一致性</b>：输出里出现的 riskScore / predictedScore 必须与库中真值一致
 *       （真值由编排器在跑流水线前写入 {@code ctx} 的 attributes，键见常量）。</li>
 * </ol>
 *
 * 纯函数实现，便于单元测试；不通过校验时由调用方决定"重写（反思）"还是"照常返回但标记"。
 */
public final class AgentOutputValidator {

    /** 编排器注入的真值键 */
    public static final String GT_RISK_SCORE = "groundTruth.riskScore";
    public static final String GT_PREDICTED_SCORE = "groundTruth.predictedScore";

    private static final double SCORE_TOLERANCE = 1.0;

    private static final Set<String> PROFILE_LEVELS = Set.of("优秀", "良好", "一般", "薄弱", "严重");
    private static final Set<String> HABIT_LEVELS = Set.of("自律型", "规律型", "波动型", "拖延型", "放弃型");
    private static final Set<String> GOAL_LEVELS = Set.of("冲刺", "稳定", "追赶", "保底");
    private static final Set<String> FEASIBILITY = Set.of("高", "中", "低");
    private static final Set<String> STRATEGY_ACTIONS = Set.of(
            "CONTINUE", "ADJUST_DIFFICULTY", "ADJUST_FOCUS", "ESCALATE", "DOWNGRADE",
            "ADJUST", "UPGRADE", "KEEP", "DEESCALATE");

    private AgentOutputValidator() {
    }

    /** 校验结果：PASS / WARN / FAIL */
    public record Result(String status, List<String> errors, List<String> warnings) {

        public boolean pass() {
            return "PASS".equals(status);
        }

        /** 供落库的简短说明 */
        public String detail() {
            if (errors.isEmpty() && warnings.isEmpty()) {
                return "OK";
            }
            String text = String.join("; ", errors.isEmpty() ? warnings : errors);
            return text.length() <= 480 ? text : text.substring(0, 479) + "…";
        }
    }

    public static Result validate(String agentName, Map<String, Object> output, AgentContext ctx) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (output == null || output.isEmpty()) {
            errors.add("输出为空");
            return new Result("FAIL", errors, warnings);
        }

        switch (agentName == null ? "" : agentName) {
            case "ProfileAgent" -> validateProfile(output, errors, warnings);
            case "RecommendAgent" -> validateRecommend(output, errors, warnings);
            case "StrategyAgent" -> validateStrategy(output, errors, warnings);
            case "MonitorAgent" -> validateMonitor(output, errors, warnings);
            case "AnalysisAgent" -> validateAnalysis(output, errors, warnings);
            case "DataQueryAgent" -> validateDataQuery(output, errors, warnings);
            default -> { /* 其它智能体只做通用校验 */ }
        }

        validateGroundTruth(output, ctx, errors);

        String status = !errors.isEmpty() ? "FAIL" : (!warnings.isEmpty() ? "WARN" : "PASS");
        return new Result(status, errors, warnings);
    }

    // ==================== 各智能体结构校验 ====================

    private static void validateProfile(Map<String, Object> output, List<String> errors, List<String> warnings) {
        Map<String, Object> knowledge = asMap(output.get("knowledgeProfile"));
        Map<String, Object> habit = asMap(output.get("habitProfile"));
        Map<String, Object> goal = asMap(output.get("goalProfile"));
        if (knowledge == null && habit == null && goal == null) {
            errors.add("三个维度画像均缺失");
            return;
        }
        // 只给出部分维度也算质量问题（交给反思环去补齐）
        if (knowledge == null || habit == null || goal == null) {
            List<String> missingDims = new ArrayList<>();
            if (knowledge == null) missingDims.add("knowledgeProfile");
            if (habit == null) missingDims.add("habitProfile");
            if (goal == null) missingDims.add("goalProfile");
            warnings.add("画像维度缺失: " + missingDims);
        }
        requireLevel(knowledge, "knowledgeProfile", PROFILE_LEVELS, warnings);
        requireLevel(habit, "habitProfile", HABIT_LEVELS, warnings);
        requireLevel(goal, "goalProfile", GOAL_LEVELS, warnings);
        if (goal != null) {
            String feasibility = str(goal.get("feasibility"));
            if (feasibility != null && !FEASIBILITY.contains(feasibility)) {
                warnings.add("goalProfile.feasibility 取值异常: " + feasibility);
            }
        }
        if (knowledge != null) {
            Double pct = num(knowledge.get("masteryPercent"));
            if (pct != null && (pct < 0 || pct > 100)) {
                errors.add("knowledgeProfile.masteryPercent 超出 0~100: " + pct);
            }
        }
    }

    private static void validateRecommend(Map<String, Object> output, List<String> errors, List<String> warnings) {
        Map<String, Object> plan = asMap(output.get("plan"));
        Object recs = output.get("recommendations");
        boolean hasRecs = recs instanceof List<?> list && !list.isEmpty();
        if (plan == null && !hasRecs) {
            errors.add("既没有 plan 也没有 recommendations");
            return;
        }
        if (plan != null && plan.isEmpty()) {
            errors.add("plan 为空对象");
        } else if (plan != null) {
            boolean hasStage = nonEmpty(plan.get("stages")) || nonEmpty(plan.get("milestones"))
                    || nonEmpty(plan.get("phases")) || nonEmpty(plan.get("weeks"));
            if (!hasStage) {
                warnings.add("plan 缺少阶段/里程碑字段");
            }
        }
    }

    private static void validateStrategy(Map<String, Object> output, List<String> errors, List<String> warnings) {
        String action = str(output.get("action"));
        if (action == null) {
            errors.add("缺少 action");
        } else if (!STRATEGY_ACTIONS.contains(action.toUpperCase())) {
            warnings.add("action 取值异常: " + action);
        }
        Double confidence = num(output.get("confidence"));
        if (confidence != null && (confidence < 0 || confidence > 1)) {
            errors.add("confidence 超出 0~1: " + confidence);
        }
    }

    private static void validateMonitor(Map<String, Object> output, List<String> errors, List<String> warnings) {
        boolean hasDims = nonEmpty(output.get("riskDimensions"));
        boolean hasSummary = str(output.get("summary")) != null;
        if (!hasDims && !hasSummary) {
            errors.add("riskDimensions 与 summary 均为空");
        } else if (!hasDims) {
            warnings.add("缺少 riskDimensions");
        }
    }

    private static void validateAnalysis(Map<String, Object> output, List<String> errors, List<String> warnings) {
        boolean hasCategory = str(output.get("weaknessCategory")) != null;
        boolean hasCause = str(output.get("rootCause")) != null;
        if (!hasCategory && !hasCause) {
            warnings.add("weaknessCategory 与 rootCause 均缺失");
        }
    }

    /** 工具调用智能体：必须给出答案，且用了工具就要留下调用痕迹 */
    private static void validateDataQuery(Map<String, Object> output, List<String> errors, List<String> warnings) {
        if (str(output.get("answer")) == null) {
            errors.add("缺少 answer");
        }
        if (Boolean.TRUE.equals(output.get("toolUsed")) && !nonEmpty(output.get("usedTools"))) {
            warnings.add("标记使用了工具但没有记录工具名");
        }
        Object rounds = output.get("rounds");
        if (rounds instanceof Number n && n.intValue() < 1) {
            warnings.add("rounds 异常: " + n);
        }
    }

    // ==================== 真值一致性 ====================
    private static void validateGroundTruth(Map<String, Object> output, AgentContext ctx, List<String> errors) {
        if (ctx == null) {
            return;
        }
        compare(output.get("riskScore"), ctx.getAttribute(GT_RISK_SCORE), "riskScore", errors);
        compare(output.get("predictedScore"), ctx.getAttribute(GT_PREDICTED_SCORE), "predictedScore", errors);
    }

    private static void compare(Object claimedRaw, Object truthRaw, String field, List<String> errors) {
        Double claimed = num(claimedRaw);
        Double truth = num(truthRaw);
        if (claimed == null || truth == null) {
            return; // 没有声明或不掌握真值时不做一致性判断
        }
        if (Math.abs(claimed - truth) > SCORE_TOLERANCE) {
            errors.add(field + " 与库中真值不一致: LLM=" + claimed + ", DB=" + truth);
        }
    }

    // ==================== 工具 ====================

    private static void requireLevel(Map<String, Object> node, String field, Set<String> allowed, List<String> warnings) {
        if (node == null) {
            return;
        }
        String level = str(node.get("level"));
        if (level == null) {
            warnings.add(field + ".level 缺失");
        } else if (!allowed.contains(level)) {
            warnings.add(field + ".level 取值异常: " + level);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    private static boolean nonEmpty(Object value) {
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }
        return value != null && !String.valueOf(value).isBlank();
    }

    private static String str(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? null : s;
    }

    private static Double num(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
