package com.example.academic_risk_warning.agent;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * P2-5 CriticRules：最小证据集 + 空转检测。
 *
 * <p>每个意图声明最小证据 key；工具结果缺证据时 Critic 要求取证而不是硬答。
 * 空转检测：连续两次相同工具 + 相同参数 → 判失败（防止 Agent 原地打转）。</p>
 */
@Component
public class CriticRules {

    public record ToolRun(String tool, Map<String, Object> args, Map<String, Object> evidence) {
    }

    public record Result(boolean passed, boolean asksEvidence, boolean idleLoop,
                         List<String> missing, String message) {
    }

    private static final Map<String, List<String>> REQUIRED = Map.of(
            "CLASS_STATS", List.of("total", "red", "yellow", "riskRatio"),
            "TREND", List.of("points"),
            "COURSE_COMPARE", List.of("rows"),
            "STUDENT_RISK", List.of("riskLevel", "riskScore"),
            "INTERVENTION", List.of("interventions"),
            "COURSE_QA", List.of("hits")
    );

    public Result evaluate(String intent, List<ToolRun> steps) {
        if (steps == null || steps.isEmpty()) {
            return new Result(false, true, false, List.of("tools"),
                    "没有工具证据，请先执行工具");
        }
        boolean idle = false;
        for (int i = 1; i < steps.size(); i++) {
            if (steps.get(i - 1).tool().equals(steps.get(i).tool())
                    && steps.get(i - 1).args().equals(steps.get(i).args())) {
                idle = true;
                break;
            }
        }
        Map<String, Object> merged = new LinkedHashMap<>();
        for (ToolRun s : steps) {
            if (s.evidence() != null) {
                merged.putAll(s.evidence());
            }
        }
        List<String> missing = new ArrayList<>();
        for (String key : REQUIRED.getOrDefault(intent, List.of())) {
            if (!merged.containsKey(key) || merged.get(key) == null) {
                missing.add(key);
            }
        }
        // 课程问答：无命中或门控拒答时要求补充范围
        boolean noHits = "COURSE_QA".equals(intent)
                && (merged.get("hits") == null || ((Number) merged.get("hits")).intValue() == 0
                || Boolean.TRUE.equals(merged.get("refused")));
        boolean passed = !idle && missing.isEmpty() && !noHits;
        String message;
        if (idle) {
            message = "检测到空转：连续两次相同工具+参数，请更换工具或参数";
        } else if (!missing.isEmpty()) {
            message = "证据不足，需要补充：" + String.join(",", missing);
        } else if (noHits) {
            message = "课程检索无命中/被门控拒答，请补充课程或问题范围";
        } else {
            message = "证据满足最小集";
        }
        return new Result(passed, !passed, idle, missing, message);
    }

    public Set<String> requiredKeys(String intent) {
        return new LinkedHashSet<>(REQUIRED.getOrDefault(intent, List.of()));
    }
}
