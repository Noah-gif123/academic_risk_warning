package com.example.academic_risk_warning.agent.tool;

import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.core.BaseAgent;
import com.example.academic_risk_warning.llm.LLMClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据查询智能体（W3 工具调用）：教师用自然语言问学情，智能体自己去"查库"再回答。
 *
 * <p>与其它智能体的区别：它不靠 prompt 里塞满数据，而是通过 {@link ToolCallingLoop}
 * 按需调用真实查询工具（成绩、预警、画像、趋势、记忆…），因此回答里的每个数字都可追溯，
 * 也是本项目"智能体 + 工具"能力的演示入口。
 */
@Component
public class DataQueryAgent extends BaseAgent<Map<String, Object>> {

    private static final String SYSTEM_PROMPT = """
            你是高校学业预警系统的「学情数据查询助手」，服务对象是任课教师与辅导员。
            你可以调用系统提供的查询工具获取真实的学情数据，再基于数据回答教师的问题。

            规则：
            1. 只依据工具返回的数据作答，严禁编造分数、预警等级、知识点或趋势；
            2. 数据不足时明确说明「当前数据不足，无法判断」，并建议需要补充什么数据；
            3. 面向教师，先给结论，再列关键数据支撑，300 字以内，不要输出 Markdown 表格；
            4. 与学生学情无关的问题，直接说明本助手只处理学情数据问题。
            """;

    private final ToolCallingLoop toolCallingLoop;

    public DataQueryAgent(LLMClient llmClient, ToolCallingLoop toolCallingLoop) {
        super(llmClient, "DataQueryAgent");
        this.toolCallingLoop = toolCallingLoop;
    }

    @Override
    protected Map<String, Object> doExecute(AgentContext ctx) {
        String question = ctx.getAttribute("question");
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("缺少 question 参数");
        }

        ToolCallingLoop.Outcome outcome = toolCallingLoop.run(llmClient, SYSTEM_PROMPT, question, ctx);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("question", question);
        data.put("answer", outcome.answer() == null ? "" : outcome.answer());
        data.put("evidence", outcome.evidence());
        data.put("toolUsed", outcome.toolUsed());
        data.put("usedTools", outcome.toolNames());
        data.put("rounds", outcome.rounds());
        data.put("toolTrace", toTrace(outcome.trace()));
        return data;
    }

    private List<Map<String, Object>> toTrace(List<ToolCallingLoop.ToolCall> trace) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ToolCallingLoop.ToolCall call : trace) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", call.name());
            item.put("arguments", call.arguments());
            item.put("success", call.success());
            item.put("durationMs", call.durationMs());
            item.put("observation", truncate(call.observation(), 300));
            if (call.error() != null) {
                item.put("error", call.error());
            }
            list.add(item);
        }
        return list;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    @Override
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    public String buildUserPrompt(AgentContext ctx) {
        String question = ctx.getAttribute("question");
        return question == null ? "" : question;
    }
}
