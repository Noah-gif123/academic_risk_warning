package com.example.academic_risk_warning.agent.tool;

import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.core.JsonUtils;
import com.example.academic_risk_warning.config.AgentProperties;
import com.example.academic_risk_warning.llm.LLMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具调用环（W3，ReAct 风格 Function Calling）。
 *
 * <p>大模型先看"工具清单"，再决定是否输出
 * {@code {"tool_calls":[{"name":"...","arguments":{...}}]}}；
 * Java 侧执行真实查询，把观察结果回灌进对话，模型据此继续调用或给出最终答案。
 * 全程不使用任何"模型编造的数据"：所有学情数字都来自工具返回。
 *
 * <p>为保证可离线测试与额度可控：
 * <ul>
 *   <li>轮次上限由 {@code agent.tool-max-rounds} 控制；</li>
 *   <li>工具异常不抛出，而是作为"观察结果"回灌，让模型自行纠错；</li>
 *   <li>未知工具名同样回灌候选清单；</li>
 *   <li>模型不按格式输出时，退化为"把原文当答案"，不报错。</li>
 * </ul>
 */
@Component
public class ToolCallingLoop {

    private static final Logger log = LoggerFactory.getLogger(ToolCallingLoop.class);

    /** 单个工具结果回灌给模型的最大长度，防止 prompt 爆炸 */
    private static final int RESULT_MAX = 1500;

    private static final String PROTOCOL = """
            【输出协议】
            1) 需要更多数据时，只输出 JSON：{"tool_calls":[{"name":"工具名","arguments":{"参数名":"值"}}]}（可一次调用多个工具）
            2) 信息足够时，只输出 JSON：{"answer":"最终答复","evidence":["引用的关键数据"],"usedTools":["用过的工具名"]}
            3) 不要输出与 JSON 无关的解释文字；不要编造工具没返回的数据。
            """;

    private final ToolRegistry registry;
    private final AgentProperties agentProperties;

    public ToolCallingLoop(ToolRegistry registry, AgentProperties agentProperties) {
        this.registry = registry;
        this.agentProperties = agentProperties;
    }

    /** 一次工具调用的留痕 */
    public record ToolCall(String name, Map<String, Object> arguments, boolean success,
                           long durationMs, String observation, String error) {}

    /** 工具调用环结果 */
    public record Outcome(String answer, List<String> evidence, List<ToolCall> trace, int rounds) {
        public boolean toolUsed() {
            return !trace.isEmpty();
        }

        public List<String> toolNames() {
            return trace.stream().map(ToolCall::name).distinct().toList();
        }
    }

    /**
     * 跑一次"提问 → 调工具 → 回答"。
     *
     * @param llmClient    大模型客户端
     * @param systemPrompt 角色提示词
     * @param question     用户问题
     * @param ctx          上下文（提供 studentId / courseId 兜底）
     */
    public Outcome run(LLMClient llmClient, String systemPrompt, String question, AgentContext ctx) {
        int maxRounds = maxRounds();
        List<ToolCall> trace = new ArrayList<>();

        StringBuilder transcript = new StringBuilder();
        transcript.append("【可用工具】\n").append(registry.catalog()).append("\n");
        transcript.append("【当前学生】studentId=").append(ctx.getStudentId())
                  .append("，courseId=").append(ctx.getCourseId() == null ? "未指定" : ctx.getCourseId()).append("\n\n");
        transcript.append("【用户问题】\n").append(question).append("\n\n");
        transcript.append(PROTOCOL);

        String answer = null;
        List<String> evidence = List.of();
        int rounds = 0;

        while (rounds < maxRounds) {
            rounds++;
            String raw = llmClient.chat(systemPrompt, transcript.toString());
            Map<String, Object> parsed = JsonUtils.parse(raw);
            List<Map<String, Object>> calls = extractCalls(parsed);

            if (calls.isEmpty()) {
                answer = extractAnswer(parsed, raw);
                evidence = extractEvidence(parsed);
                break;
            }

            for (Map<String, Object> call : calls) {
                ToolCall executed = execute(call, ctx);
                trace.add(executed);
                transcript.append("\n【工具调用】").append(executed.name())
                          .append(" 参数=").append(JsonUtils.toJsonString(executed.arguments())).append("\n")
                          .append("观察结果：").append(executed.observation()).append("\n");
            }
            transcript.append("\n若信息已足够，请直接输出 {\"answer\":...}；否则继续调用工具。\n");
        }

        if (answer == null) {
            // 轮次用尽仍有工具调用：再要一次最终答案（不放开新的工具调用）
            String raw = llmClient.chat(systemPrompt,
                    transcript + "\n工具调用轮次已达上限，请勿再调用工具，直接基于以上观察输出 {\"answer\":...}。");
            Map<String, Object> parsed = JsonUtils.parse(raw);
            answer = extractAnswer(parsed, raw);
            evidence = extractEvidence(parsed);
            rounds++;
        }

        log.info("[ToolCallingLoop] 完成：轮次={}，工具调用={}，答案长度={}",
                rounds, trace.size(), answer == null ? 0 : answer.length());
        return new Outcome(answer, evidence, trace, rounds);
    }

    // ==================== 内部 ====================

    private ToolCall execute(Map<String, Object> call, AgentContext ctx) {
        String name = call.get("name") == null ? "" : String.valueOf(call.get("name")).trim();
        Map<String, Object> args = asStringMap(call.get("arguments"));
        AgentTool tool = registry.get(name);

        long start = System.currentTimeMillis();
        if (tool == null) {
            long ms = System.currentTimeMillis() - start;
            String error = "未找到名为 " + name + " 的工具";
            return new ToolCall(name, args, false, ms,
                    JsonUtils.toJsonString(Map.of("error", error, "availableTools", registry.names())), error);
        }

        try {
            Map<String, Object> result = tool.invoke(ctx, args);
            long ms = System.currentTimeMillis() - start;
            Map<String, Object> safe = result == null ? Map.of("result", "该工具没有返回数据") : result;
            return new ToolCall(name, args, true, ms, truncate(JsonUtils.toJsonString(safe), RESULT_MAX), null);
        } catch (Exception e) {
            long ms = System.currentTimeMillis() - start;
            log.warn("[ToolCallingLoop] 工具 {} 执行失败: {}", name, e.getMessage());
            String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return new ToolCall(name, args, false, ms,
                    JsonUtils.toJsonString(Map.of("error", "工具执行失败：" + error)), error);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractCalls(Map<String, Object> parsed) {
        List<Map<String, Object>> calls = new ArrayList<>();
        Object toolCalls = parsed.get("tool_calls");
        if (toolCalls instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    Map<String, Object> call = new LinkedHashMap<>();
                    Object name = m.get("name") != null ? m.get("name") : m.get("tool");
                    call.put("name", name);
                    call.put("arguments", m.get("arguments") != null ? m.get("arguments") : m.get("args"));
                    if (call.get("name") != null) {
                        calls.add(call);
                    }
                }
            }
        } else if (parsed.get("tool") instanceof String single) {
            // 兼容单工具写法 {"tool":"x","arguments":{...}}
            Map<String, Object> call = new LinkedHashMap<>();
            call.put("name", single);
            call.put("arguments", parsed.get("arguments"));
            calls.add(call);
        }
        return calls;
    }

    private String extractAnswer(Map<String, Object> parsed, String raw) {
        Object answer = parsed.get("answer");
        if (answer == null) answer = parsed.get("final_answer");
        if (answer == null) answer = parsed.get("reply");
        if (answer != null) {
            return String.valueOf(answer).trim();
        }
        if (parsed.get("raw") != null) {
            return String.valueOf(parsed.get("raw")).trim();
        }
        return raw == null ? "" : raw.trim();
    }

    private List<String> extractEvidence(Map<String, Object> parsed) {
        Object evidence = parsed.get("evidence");
        if (evidence == null) evidence = parsed.get("key_points");
        if (evidence instanceof List<?> list) {
            return list.stream().filter(java.util.Objects::nonNull).map(String::valueOf).toList();
        }
        return List.of();
    }

    private Map<String, Object> asStringMap(Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> m) {
            m.forEach((k, v) -> result.put(String.valueOf(k), v));
        }
        return result;
    }

    private int maxRounds() {
        int configured = agentProperties.getToolMaxRounds();
        return Math.max(1, Math.min(configured, 5));
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
