package com.example.academic_risk_warning.llm;

import com.example.academic_risk_warning.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 百炼 Chat 客户端：对接阿里云百炼兼容 OpenAI 格式的 Chat API
 * 用于所有 Agent 的通用 LLM 对话（意图识别、风险分析、画像、推荐、策略等）
 */
@Primary
@Component
public class BailianChatClient implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(BailianChatClient.class);

    @Value("${agent.bailian-api-key:}")
    private String apiKey;

    @Value("${agent.bailian-chat-model:qwen-plus}")
    private String model;

    @Value("${agent.bailian-base-url:https://dashscope.aliyuncs.com}")
    private String baseUrl;

    private final AgentProperties agentProperties;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public BailianChatClient(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", userPrompt));
        return chat(systemPrompt, messages);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String chat(String systemPrompt, List<Map<String, Object>> messages) {
        if (!agentProperties.isEnabled()) {
            throw new IllegalStateException("多智能体功能已关闭（agent.enabled=false）");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("百炼 API Key 未配置");
        }

        List<Map<String, Object>> fullMessages = new ArrayList<>();
        fullMessages.add(Map.of("role", "system", "content", systemPrompt));
        fullMessages.addAll(messages);

        String requestBody = buildRequestBody(fullMessages);

        try {
            // 百炼兼容模式 API：OpenAI 格式
            String url = baseUrl + "/compatible-mode/v1/chat/completions";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(effectiveTimeoutSeconds()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("百炼 Chat API 返回错误 ({}): {}", response.statusCode(), response.body());
                throw new RuntimeException("百炼 Chat API 返回错误 (" + response.statusCode() + ")");
            }

            return parseReply(response.body());
        } catch (Exception e) {
            log.error("百炼 Chat API 调用失败", e);
            throw new RuntimeException("百炼 Chat API 调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String ragQuery(String question) {
        // 百炼 Chat 模型不内置 RAG，回退到通用对话
        return chat("你是一个知识渊博的学习助手，请基于你的知识回答学生的问题。", question);
    }

    @Override
    public String ragQuery(String question, Long courseId) {
        // Chat 模型不支持课程级 RAG 路由，回退到通用对话
        return ragQuery(question);
    }

    /**
     * 单次调用超时：以 {@code agent.llm-timeout} 为准，同时不超过 {@code agent.agent-timeout}。
     * （单个智能体通常只发起一次 LLM 调用，因此这个上限也等价于该步的耗时上限；
     * 多次调用的整体预算会随异步任务中心一起做。）
     */
    private long effectiveTimeoutSeconds() {
        int llm = agentProperties.getLlmTimeout();
        int agent = agentProperties.getAgentTimeout();
        int seconds = agent > 0 ? Math.min(llm, agent) : llm;
        return Math.max(1, seconds);
    }

    private String buildRequestBody(List<Map<String, Object>> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(escape(model)).append("\",");
        sb.append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) sb.append(",");
            Map<String, Object> msg = messages.get(i);
            sb.append("{\"role\":\"").append(escape((String) msg.get("role"))).append("\",");
            sb.append("\"content\":\"").append(escape((String) msg.get("content"))).append("\"}");
        }
        sb.append("],\"temperature\":0.7,\"max_tokens\":1024}");
        return sb.toString();
    }

    private String parseReply(String json) {
        int idx = json.indexOf("\"content\"");
        if (idx < 0) return "（AI 未返回内容）";
        idx = json.indexOf(":", idx) + 1;
        while (idx < json.length() && (json.charAt(idx) == ' ' || json.charAt(idx) == '"')) idx++;
        StringBuilder content = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx);
            if (c == '\\') {
                idx++;
                if (idx < json.length()) {
                    char next = json.charAt(idx);
                    switch (next) {
                        case '"': content.append('"'); break;
                        case '\\': content.append('\\'); break;
                        case 'n': content.append('\n'); break;
                        case 't': content.append('\t'); break;
                        case 'r': content.append('\r'); break;
                        default: content.append('\\').append(next);
                    }
                }
                idx++;
            } else if (c == '"') {
                break;
            } else {
                content.append(c);
                idx++;
            }
        }
        return content.length() > 0 ? content.toString() : "（空回复）";
    }

    private String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
