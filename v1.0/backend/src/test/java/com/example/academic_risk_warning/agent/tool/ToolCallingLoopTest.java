package com.example.academic_risk_warning.agent.tool;

import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.config.AgentProperties;
import com.example.academic_risk_warning.llm.LLMClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * W3 工具调用环测试：模型按协议要求调工具 → Java 侧真实执行 → 观察结果回灌 → 最终作答。
 * 覆盖正常调用、直接作答、未知工具、工具异常、轮次上限五种情况。
 */
@DisplayName("工具调用环测试")
class ToolCallingLoopTest {

    /** 记录调用次数的假 LLM：按顺序返回预置回复 */
    private static class ScriptedLlm implements LLMClient {
        private final List<String> replies;
        final List<String> userPrompts = new ArrayList<>();
        int calls = 0;

        ScriptedLlm(String... replies) {
            this.replies = List.of(replies);
        }

        @Override
        public String chat(String systemPrompt, String userPrompt) {
            userPrompts.add(userPrompt);
            String reply = replies.get(Math.min(calls, replies.size() - 1));
            calls++;
            return reply;
        }

        @Override
        public String chat(String systemPrompt, List<Map<String, Object>> messages) {
            return chat(systemPrompt, String.valueOf(messages));
        }

        @Override
        public String ragQuery(String question) {
            return "unused";
        }
    }

    /** 假工具：记录收到的参数，返回固定结构 */
    private static class FakeTool implements AgentTool {
        final List<Map<String, Object>> received = new ArrayList<>();
        private final String name;
        private final boolean fail;

        FakeTool(String name, boolean fail) {
            this.name = name;
            this.fail = fail;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "测试用工具";
        }

        @Override
        public Map<String, Object> invoke(AgentContext ctx, Map<String, Object> args) {
            received.add(args);
            if (fail) {
                throw new IllegalStateException("数据库连接失败");
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("riskScore", 72.5);
            result.put("studentId", ctx.getStudentId());
            return result;
        }
    }

    private ToolCallingLoop loop(List<AgentTool> tools, AgentProperties properties) {
        return new ToolCallingLoop(new ToolRegistry(tools), properties);
    }

    private AgentContext ctx() {
        AgentContext ctx = new AgentContext(2L);
        ctx.setCourseId(1L);
        return ctx;
    }

    @Test
    @DisplayName("模型先调工具再作答 → 工具真实执行、参数透传、轨迹留痕")
    void shouldExecuteToolThenAnswer() {
        FakeTool tool = new FakeTool("student_risk_profile", false);
        ScriptedLlm llm = new ScriptedLlm(
                "{\"tool_calls\":[{\"name\":\"student_risk_profile\",\"arguments\":{\"courseId\":1}}]}",
                "{\"answer\":\"该生风险分 72.5，属于高风险\",\"evidence\":[\"riskScore=72.5\"]}");

        ToolCallingLoop.Outcome outcome = loop(List.of(tool), new AgentProperties()).run(llm, "SYS", "这个学生风险多高？", ctx());

        assertEquals("该生风险分 72.5，属于高风险", outcome.answer());
        assertEquals(List.of("riskScore=72.5"), outcome.evidence());
        assertTrue(outcome.toolUsed());
        assertEquals(List.of("student_risk_profile"), outcome.toolNames());
        assertEquals(2, outcome.rounds());
        assertEquals(1, tool.received.size(), "工具应被真实调用一次");
        assertEquals(1L, ((Number) tool.received.get(0).get("courseId")).longValue());
        // 观察结果必须回灌给模型（第二轮的 prompt 里含工具返回的真实数据）
        assertTrue(llm.userPrompts.get(1).contains("72.5"), llm.userPrompts.get(1));
        assertTrue(llm.userPrompts.get(0).contains("student_risk_profile"), "第一轮 prompt 应包含工具清单");
    }

    @Test
    @DisplayName("模型认为信息已足够 → 不调用任何工具，单轮结束")
    void shouldAnswerWithoutTools() {
        FakeTool tool = new FakeTool("student_risk_profile", false);
        ScriptedLlm llm = new ScriptedLlm("{\"answer\":\"请提供学生ID\"}");

        ToolCallingLoop.Outcome outcome = loop(List.of(tool), new AgentProperties()).run(llm, "SYS", "他怎么样？", ctx());

        assertFalse(outcome.toolUsed());
        assertEquals(1, outcome.rounds());
        assertEquals(1, llm.calls);
        assertTrue(tool.received.isEmpty());
    }

    @Test
    @DisplayName("模型调用不存在的工具 → 回灌候选清单而不是抛异常")
    void shouldFeedBackUnknownTool() {
        FakeTool tool = new FakeTool("student_courses", false);
        ScriptedLlm llm = new ScriptedLlm(
                "{\"tool_calls\":[{\"name\":\"drop_database\",\"arguments\":{}}]}",
                "{\"answer\":\"我只能查询学情数据\"}");

        ToolCallingLoop.Outcome outcome = loop(List.of(tool), new AgentProperties()).run(llm, "SYS", "删库", ctx());

        assertEquals(1, outcome.trace().size());
        assertFalse(outcome.trace().get(0).success());
        assertTrue(outcome.trace().get(0).observation().contains("student_courses"), outcome.trace().get(0).observation());
        assertTrue(llm.userPrompts.get(1).contains("drop_database"));
        assertEquals("我只能查询学情数据", outcome.answer());
    }

    @Test
    @DisplayName("工具执行抛异常 → 记为失败并把错误回灌，仍能给出答案")
    void shouldSurviveToolFailure() {
        FakeTool broken = new FakeTool("student_score_info", true);
        ScriptedLlm llm = new ScriptedLlm(
                "{\"tool_calls\":[{\"name\":\"student_score_info\",\"arguments\":{}}]}",
                "{\"answer\":\"成绩数据暂时取不到\"}");

        ToolCallingLoop.Outcome outcome = loop(List.of(broken), new AgentProperties()).run(llm, "SYS", "他成绩怎么样？", ctx());

        assertFalse(outcome.trace().get(0).success());
        assertTrue(outcome.trace().get(0).observation().contains("数据库连接失败"), outcome.trace().get(0).observation());
        assertEquals("成绩数据暂时取不到", outcome.answer());
    }

    @Test
    @DisplayName("模型一直要求调工具 → 到轮次上限后强制收口给答案")
    void shouldForceFinalAnswerAfterMaxRounds() {
        FakeTool tool = new FakeTool("student_alerts", false);
        AgentProperties properties = new AgentProperties();
        properties.setToolMaxRounds(2);
        ScriptedLlm llm = new ScriptedLlm(
                "{\"tool_calls\":[{\"name\":\"student_alerts\",\"arguments\":{}}]}",
                "{\"tool_calls\":[{\"name\":\"student_alerts\",\"arguments\":{}}]}",
                "{\"answer\":\"该生共 3 条历史预警\"}");

        ToolCallingLoop.Outcome outcome = loop(List.of(tool), properties).run(llm, "SYS", "预警几次？", ctx());

        assertEquals("该生共 3 条历史预警", outcome.answer());
        assertEquals(2, outcome.trace().size(), "两次工具调用都应留痕");
        assertEquals(3, llm.calls, "两次工具轮 + 一次强制收口");
        assertTrue(llm.userPrompts.get(2).contains("已达上限"), llm.userPrompts.get(2));
    }

    @Test
    @DisplayName("模型不按协议输出（纯文本）→ 退化为原文答案，不报错")
    void shouldFallBackToPlainText() {
        FakeTool tool = new FakeTool("student_courses", false);
        ScriptedLlm llm = new ScriptedLlm("这个学生目前没有明显风险，建议继续观察。");

        ToolCallingLoop.Outcome outcome = loop(List.of(tool), new AgentProperties()).run(llm, "SYS", "他危险吗？", ctx());

        assertNotNull(outcome.answer());
        assertTrue(outcome.answer().contains("继续观察"), outcome.answer());
        assertEquals(1, llm.calls);
    }

    @Test
    @DisplayName("工具返回超长内容会被裁剪，避免 prompt 爆炸")
    void shouldTruncateLongObservation() {
        AgentTool verbose = new AgentTool() {
            @Override
            public String name() {
                return "verbose_tool";
            }

            @Override
            public String description() {
                return "返回超长内容的工具";
            }

            @Override
            public Map<String, Object> invoke(AgentContext ctx, Map<String, Object> args) {
                return Map.of("blob", "x".repeat(5000));
            }
        };
        ScriptedLlm llm = new ScriptedLlm(
                "{\"tool_calls\":[{\"name\":\"verbose_tool\",\"arguments\":{}}]}",
                "{\"answer\":\"ok\"}");

        ToolCallingLoop.Outcome outcome = loop(List.of(verbose), new AgentProperties()).run(llm, "SYS", "长数据", ctx());

        String observation = outcome.trace().get(0).observation();
        assertTrue(observation.length() <= 1500, "观察结果应被裁剪，实际=" + observation.length());
        assertTrue(observation.endsWith("…"), observation.substring(observation.length() - 5));
    }
}
