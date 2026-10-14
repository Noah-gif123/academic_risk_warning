package com.example.academic_risk_warning.agent.tool;

import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.core.AgentOutputValidator;
import com.example.academic_risk_warning.agent.core.AgentResult;
import com.example.academic_risk_warning.agent.core.AgentRunContext;
import com.example.academic_risk_warning.config.AgentProperties;
import com.example.academic_risk_warning.llm.LLMClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DataQueryAgent（工具调用智能体）测试：
 * 输出结构包含答案与工具轨迹；缺 question 时明确失败；输出校验规则生效。
 */
@DisplayName("数据查询智能体（工具调用）测试")
class DataQueryAgentTest {

    private static class CourseTool implements AgentTool {
        @Override
        public String name() {
            return "student_courses";
        }

        @Override
        public String description() {
            return "查询该学生选修的全部课程";
        }

        @Override
        public Map<String, Object> invoke(AgentContext ctx, Map<String, Object> args) {
            return Map.of("courseCount", 2, "courses", List.of(
                    Map.of("courseId", 1, "courseName", "数据结构"),
                    Map.of("courseId", 2, "courseName", "高等数学")));
        }
    }

    private DataQueryAgent agent(LLMClient llm) {
        ToolCallingLoop loop = new ToolCallingLoop(new ToolRegistry(List.of(new CourseTool())), new AgentProperties());
        return new DataQueryAgent(llm, loop);
    }

    private AgentContext ctx(String question) {
        AgentContext ctx = new AgentContext(2L);
        ctx.setCourseId(1L);
        if (question != null) {
            ctx.setAttribute("question", question);
        }
        return ctx;
    }

    @Test
    @DisplayName("正常问答：返回答案 + 工具轨迹，并写入运行上下文")
    void shouldReturnAnswerWithToolTrace() {
        LLMClient llm = mock(LLMClient.class);
        when(llm.chat(anyString(), anyString()))
                .thenReturn("{\"tool_calls\":[{\"name\":\"student_courses\",\"arguments\":{}}]}")
                .thenReturn("{\"answer\":\"该生选了数据结构与高等数学\",\"evidence\":[\"courseCount=2\"]}");

        AgentRunContext.begin();
        AgentResult<Map<String, Object>> result = agent(llm).execute(ctx("这个学生选了哪些课？"));
        List<AgentRunContext.StepRecord> steps = AgentRunContext.collectAndClear();

        assertTrue(result.isSuccess(), result.getErrorMessage());
        Map<String, Object> data = result.getData();
        assertEquals("该生选了数据结构与高等数学", data.get("answer"));
        assertEquals(Boolean.TRUE, data.get("toolUsed"));
        assertEquals(List.of("student_courses"), data.get("usedTools"));
        assertEquals(2, data.get("rounds"));
        assertTrue(((List<?>) data.get("toolTrace")).size() == 1);

        assertEquals(1, steps.size());
        assertEquals("DataQueryAgent", steps.get(0).agentName());
        assertEquals("PASS", steps.get(0).validationStatus(), steps.get(0).validationDetail());
        assertTrue(steps.get(0).outputFields().contains("answer"), String.valueOf(steps.get(0).outputFields()));
    }

    @Test
    @DisplayName("缺少 question → 失败且给出明确原因")
    void shouldFailWithoutQuestion() {
        AgentResult<Map<String, Object>> result = agent(mock(LLMClient.class)).execute(ctx(null));

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("question"), result.getErrorMessage());
    }

    @Test
    @DisplayName("输出校验：缺 answer 判 FAIL；用了工具却没记工具名给 WARN")
    void validatorShouldCheckAnswerAndToolTrace() {
        AgentOutputValidator.Result missingAnswer =
                AgentOutputValidator.validate("DataQueryAgent", Map.of("rounds", 2), ctx("q"));
        assertEquals("FAIL", missingAnswer.status());
        assertTrue(missingAnswer.detail().contains("answer"), missingAnswer.detail());

        AgentOutputValidator.Result ok = AgentOutputValidator.validate("DataQueryAgent",
                Map.of("answer", "结论", "toolUsed", true, "usedTools", List.of("student_courses"), "rounds", 2),
                ctx("q"));
        assertEquals("PASS", ok.status(), ok.detail());

        AgentOutputValidator.Result warn = AgentOutputValidator.validate("DataQueryAgent",
                Map.of("answer", "结论", "toolUsed", true, "rounds", 2), ctx("q"));
        assertEquals("WARN", warn.status(), warn.detail());
    }
}
