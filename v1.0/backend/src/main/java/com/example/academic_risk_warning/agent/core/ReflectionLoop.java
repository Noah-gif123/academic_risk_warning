package com.example.academic_risk_warning.agent.core;

import com.example.academic_risk_warning.config.AgentProperties;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * 可复用的反思环（Self-Refine，W3 从 RecommendAgent 抽出）。
 *
 * <p>流程：校验初稿 → 不通过且开启反思时，把校验问题当作"批评"让模型重写一版 →
 * 只有"重写后更合规"（通过校验，或错误数变少）才采用，避免越改越差。
 *
 * <p>过程通过 {@link AgentRunContext#setReflection(String, int)} 记录，
 * 最终随 {@code agent_run_step} 落库（reflection / attempts / validation_*），
 * 便于后续做"有反思 vs 无反思"的消融对比。
 */
public final class ReflectionLoop {

    /**
     * @param result   最终采用的输出
     * @param raw      最终采用的原始文本
     * @param refined  是否真的采用了重写结果
     * @param critique 批评内容（未触发反思时为 null）
     * @param attempts LLM 调用轮次
     */
    public record Outcome(Map<String, Object> result, String raw, boolean refined, String critique, int attempts) {}

    private ReflectionLoop() {
    }

    public static Outcome run(AgentContext ctx, String agentName, String firstRaw, Map<String, Object> firstResult,
                              AgentProperties properties, UnaryOperator<String> rewriter) {
        AgentOutputValidator.Result validation = AgentOutputValidator.validate(agentName, firstResult, ctx);
        boolean enabled = properties != null
                && properties.isReflectionEnabled()
                && properties.getReflectionMaxRounds() > 0;
        if (validation.pass() || !enabled) {
            return new Outcome(firstResult, firstRaw, false, null, 1);
        }

        String critique = buildCritique(validation);
        AgentRunContext.setReflection(critique, 2);

        String revisedRaw;
        try {
            revisedRaw = rewriter.apply(critique);
        } catch (Exception e) {
            return new Outcome(firstResult, firstRaw, false, critique, 2); // 重写失败保留初稿，但留痕
        }

        Map<String, Object> revised = JsonUtils.parse(revisedRaw);
        if (revised == null || revised.isEmpty() || revised.containsKey("raw")) {
            return new Outcome(firstResult, firstRaw, false, critique, 2); // 重写不可解析 → 保留初稿
        }

        AgentOutputValidator.Result revisedValidation = AgentOutputValidator.validate(agentName, revised, ctx);
        if (revisedValidation.pass() || revisedValidation.errors().size() < validation.errors().size()) {
            return new Outcome(revised, revisedRaw, true, critique, 2);
        }
        return new Outcome(firstResult, firstRaw, false, critique, 2);
    }

    /** 把校验发现的问题拼成"自检反馈"提示词 */
    public static String buildCritique(AgentOutputValidator.Result validation) {
        List<String> issues = validation.errors().isEmpty() ? validation.warnings() : validation.errors();
        return """
                === 自检反馈（请修正后重新输出完整 JSON） ===
                上一版输出存在以下问题：
                - %s
                要求：只输出修正后的完整 JSON，字段与格式必须与上面要求完全一致，不要输出任何解释性文字。
                """.formatted(String.join("\n- ", issues));
    }
}
