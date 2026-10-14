package com.example.academic_risk_warning.agent.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 智能体输出校验器测试：结构 / 取值 / 真值一致性三层。
 */
@DisplayName("智能体输出校验器测试")
class AgentOutputValidatorTest {

    @Test
    @DisplayName("画像输出完整且合法 → PASS（未提供真值时不判一致性）")
    void validProfileShouldPass() {
        Map<String, Object> output = Map.of(
                "knowledgeProfile", Map.of("level", "薄弱", "summary", "基础不牢", "masteryPercent", 55),
                "habitProfile", Map.of("level", "拖延型", "summary", "响应延迟"),
                "goalProfile", Map.of("level", "追赶", "summary", "目标合理", "feasibility", "中"));

        AgentOutputValidator.Result result = AgentOutputValidator.validate("ProfileAgent", output, new AgentContext(1L));

        assertEquals("PASS", result.status());
        assertTrue(result.pass());
    }

    @Test
    @DisplayName("三个维度全缺失 → FAIL")
    void missingAllProfilesShouldFail() {
        AgentOutputValidator.Result result = AgentOutputValidator.validate(
                "ProfileAgent", Map.of("summary", "只有一句总结"), new AgentContext(1L));

        assertEquals("FAIL", result.status());
        assertTrue(result.errors().get(0).contains("三个维度画像均缺失"));
    }

    @Test
    @DisplayName("masteryPercent 超过 100 → FAIL")
    void outOfRangePercentShouldFail() {
        Map<String, Object> output = Map.of(
                "knowledgeProfile", Map.of("level", "优秀", "masteryPercent", 150));

        AgentOutputValidator.Result result = AgentOutputValidator.validate("ProfileAgent", output, new AgentContext(1L));

        assertEquals("FAIL", result.status());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("masteryPercent")));
    }

    @Test
    @DisplayName("等级/可行性不在枚举内 → WARN（不阻断）")
    void unknownEnumShouldWarn() {
        Map<String, Object> output = Map.of(
                "knowledgeProfile", Map.of("level", "挺好的"),
                "goalProfile", Map.of("level", "稳定", "feasibility", "很高"));

        AgentOutputValidator.Result result = AgentOutputValidator.validate("ProfileAgent", output, new AgentContext(1L));

        assertEquals("WARN", result.status());
        assertFalse(result.pass());
        assertTrue(result.warnings().size() >= 2, "应包含枚举取值与维度缺失告警: " + result.warnings());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("feasibility")));
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("画像维度缺失")));
    }

    @Test
    @DisplayName("数值与库中真值不一致 → FAIL（幻觉数值）")
    void inconsistentRiskScoreShouldFail() {
        AgentContext ctx = new AgentContext(2L);
        ctx.setAttribute(AgentOutputValidator.GT_RISK_SCORE, BigDecimal.valueOf(18.2));
        ctx.setAttribute(AgentOutputValidator.GT_PREDICTED_SCORE, 56.0);

        Map<String, Object> output = Map.of("riskScore", 3.5, "predictedScore", 56.4);

        AgentOutputValidator.Result result = AgentOutputValidator.validate("MonitorAgent", output, ctx);

        assertEquals("FAIL", result.status());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("riskScore 与库中真值不一致")));
    }

    @Test
    @DisplayName("数值在容差内 → 不判为不一致")
    void consistentRiskScoreShouldPassGroundTruth() {
        AgentContext ctx = new AgentContext(2L);
        ctx.setAttribute(AgentOutputValidator.GT_RISK_SCORE, 18.2);

        Map<String, Object> output = Map.of("riskScore", 18.6, "summary", "整体风险可控");

        AgentOutputValidator.Result result = AgentOutputValidator.validate("MonitorAgent", output, ctx);

        assertTrue(result.errors().isEmpty(), String.join(";", result.errors()));
    }

    @Test
    @DisplayName("推荐输出缺少 plan 与 recommendations → FAIL")
    void recommendWithoutPlanShouldFail() {
        AgentOutputValidator.Result result = AgentOutputValidator.validate(
                "RecommendAgent", Map.of("note", "无内容"), new AgentContext(2L));

        assertEquals("FAIL", result.status());
    }

    @Test
    @DisplayName("策略 action 缺失 → FAIL；action 非法 → WARN")
    void strategyActionChecks() {
        assertEquals("FAIL", AgentOutputValidator.validate(
                "StrategyAgent", Map.of("reason", "x"), new AgentContext(2L)).status());
        assertEquals("WARN", AgentOutputValidator.validate(
                "StrategyAgent", Map.of("action", "SOMETHING"), new AgentContext(2L)).status());
        assertEquals("PASS", AgentOutputValidator.validate(
                "StrategyAgent", Map.of("action", "CONTINUE"), new AgentContext(2L)).status());
    }

    @Test
    @DisplayName("空输出 → FAIL")
    void emptyOutputShouldFail() {
        assertEquals("FAIL", AgentOutputValidator.validate("ProfileAgent", Map.of(), new AgentContext(1L)).status());
    }

    @Test
    @DisplayName("detail() 截断到 480 以内")
    void detailShouldBeTruncated() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            sb.append("问题").append(i).append(";");
        }
        Map<String, Object> output = Map.of("knowledgeProfile", Map.of("level", "未知词", "summary", sb.toString()));
        AgentOutputValidator.Result result = AgentOutputValidator.validate("ProfileAgent", output, new AgentContext(1L));
        assertTrue(result.detail().length() <= 480, "detail 长度=" + result.detail().length());
    }

    @Test
    @DisplayName("真实场景形状：画像 JSON 含中文等级与嵌套数组 → PASS")
    void realisticProfileShapeShouldPass() {
        Map<String, Object> output = Map.of(
                "knowledgeProfile", Map.of(
                        "level", "良好",
                        "summary", "知识基础扎实",
                        "strongPoints", List.of("核心概念"),
                        "weakPoints", List.of("迁移能力"),
                        "masteryPercent", 78),
                "habitProfile", Map.of("level", "规律型", "traits", List.of("按时完成")),
                "goalProfile", Map.of("level", "稳定", "targetScore", 85, "feasibility", "高",
                        "milestones", List.of("2周内完成率≥50%")));

        AgentOutputValidator.Result result = AgentOutputValidator.validate("ProfileAgent", output, new AgentContext(1L));

        assertEquals("PASS", result.status());
        assertEquals("OK", result.detail());
    }
}
