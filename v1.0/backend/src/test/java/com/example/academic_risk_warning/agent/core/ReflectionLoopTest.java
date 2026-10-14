package com.example.academic_risk_warning.agent.core;

import com.example.academic_risk_warning.config.AgentProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 反思环（Self-Refine）公用组件测试：
 * 合规不重写、不合规重写并采用、越改越差不采用、重写异常保留初稿、开关关闭不重写。
 */
@DisplayName("反思环 ReflectionLoop 测试")
class ReflectionLoopTest {

    private static final String AGENT = "RecommendAgent";

    private static final String VALID = "{\"plan\":{\"stages\":[{\"name\":\"第1周\"}]},\"recommendations\":[]}";
    private static final String INVALID = "{\"summary\":\"只有一句总结\"}";

    private final AgentContext ctx = new AgentContext(2L);

    @AfterEach
    void tearDown() {
        AgentRunContext.clear();
    }

    @Test
    @DisplayName("初稿合规 → 不触发重写")
    void validDraftShouldNotRewrite() {
        AtomicInteger calls = new AtomicInteger();
        ReflectionLoop.Outcome outcome = ReflectionLoop.run(ctx, AGENT, VALID, JsonUtils.parse(VALID),
                new AgentProperties(), critique -> {
                    calls.incrementAndGet();
                    return VALID;
                });

        assertEquals(1, outcome.attempts());
        assertFalse(outcome.refined());
        assertEquals(0, calls.get());
        assertTrue(outcome.result().containsKey("plan"));
    }

    @Test
    @DisplayName("初稿不合规 → 重写并采用，记录批评与轮次")
    void invalidDraftShouldRewriteAndAdopt() {
        AgentRunContext.begin();
        ReflectionLoop.Outcome outcome = ReflectionLoop.run(ctx, AGENT, INVALID, JsonUtils.parse(INVALID),
                new AgentProperties(), critique -> {
                    assertTrue(critique.contains("plan"), "批评应指出缺失字段");
                    return VALID;
                });

        assertEquals(2, outcome.attempts());
        assertTrue(outcome.refined());
        assertNotNull(outcome.critique());
        assertTrue(outcome.result().containsKey("plan"));
        assertTrue(AgentRunContext.hasReflection(), "应把反思过程登记到运行上下文");
    }

    @Test
    @DisplayName("重写不可解析 → 保留初稿但仍记录反思")
    void unparsableRewriteShouldKeepDraft() {
        AgentRunContext.begin();
        ReflectionLoop.Outcome outcome = ReflectionLoop.run(ctx, AGENT, INVALID, JsonUtils.parse(INVALID),
                new AgentProperties(), critique -> "抱歉，我无法按要求输出");

        assertFalse(outcome.refined());
        assertEquals(2, outcome.attempts());
        assertEquals("只有一句总结", outcome.result().get("summary"));
        assertTrue(AgentRunContext.hasReflection());
    }

    @Test
    @DisplayName("重写仍不合规且没有更好 → 不采用")
    void worseRewriteShouldNotBeAdopted() {
        Map<String, Object> first = new java.util.LinkedHashMap<>(Map.of("summary", "x"));
        ReflectionLoop.Outcome outcome = ReflectionLoop.run(ctx, AGENT, "{}", first,
                new AgentProperties(), critique -> "{\"note\":\"依然没有 plan\"}");

        assertFalse(outcome.refined());
        assertEquals("x", outcome.result().get("summary"));
    }

    @Test
    @DisplayName("重写抛异常 → 保留初稿，不向上抛")
    void rewriteFailureShouldKeepDraft() {
        ReflectionLoop.Outcome outcome = ReflectionLoop.run(ctx, AGENT, INVALID, JsonUtils.parse(INVALID),
                new AgentProperties(), critique -> {
                    throw new IllegalStateException("LLM 挂了");
                });

        assertFalse(outcome.refined());
        assertEquals(2, outcome.attempts());
        assertNotNull(outcome.result().get("summary"));
    }

    @Test
    @DisplayName("关闭反思开关 → 不重写（attempts=1）")
    void disabledShouldNotRewrite() {
        AgentProperties properties = new AgentProperties();
        properties.setReflectionEnabled(false);
        AtomicInteger calls = new AtomicInteger();

        ReflectionLoop.Outcome outcome = ReflectionLoop.run(ctx, AGENT, INVALID, JsonUtils.parse(INVALID),
                properties, critique -> {
                    calls.incrementAndGet();
                    return VALID;
                });

        assertEquals(1, outcome.attempts());
        assertEquals(0, calls.get());
        assertFalse(outcome.refined());
    }

    @Test
    @DisplayName("轮次上限为 0 → 视为关闭")
    void zeroRoundsShouldBeTreatedAsDisabled() {
        AgentProperties properties = new AgentProperties();
        properties.setReflectionMaxRounds(0);

        ReflectionLoop.Outcome outcome = ReflectionLoop.run(ctx, AGENT, INVALID, JsonUtils.parse(INVALID),
                properties, critique -> VALID);

        assertEquals(1, outcome.attempts());
        assertFalse(outcome.refined());
    }

    @Test
    @DisplayName("画像类输出：某维度缺失也会触发重写")
    void profileMissingDimensionShouldRewrite() {
        String draft = "{\"knowledgeProfile\":{\"level\":\"良好\"},\"habitProfile\":{\"level\":\"规律型\"}}";
        String fixed = "{\"knowledgeProfile\":{\"level\":\"良好\"},\"habitProfile\":{\"level\":\"规律型\"},"
                + "\"goalProfile\":{\"level\":\"稳定\",\"feasibility\":\"高\"}}";

        ReflectionLoop.Outcome outcome = ReflectionLoop.run(ctx, "ProfileAgent", draft, JsonUtils.parse(draft),
                new AgentProperties(), critique -> fixed);

        assertEquals(2, outcome.attempts());
        assertTrue(outcome.result().containsKey("goalProfile"));
        assertTrue(List.of("knowledgeProfile", "habitProfile", "goalProfile")
                .stream().allMatch(outcome.result()::containsKey));
    }
}
