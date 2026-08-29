package com.example.academic_risk_warning.llm;

import com.example.academic_risk_warning.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 百炼 RAG 客户端：调用百炼应用 API 进行知识库检索+生成
 */
@Component
public class BailianRAGClient implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(BailianRAGClient.class);

    private final AgentProperties agentProperties;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public BailianRAGClient(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        // 百炼应用模式下 system prompt 在平台侧配置，这里直接走 RAG 查询
        return ragQuery(userPrompt);
    }

    @Override
    public String chat(String systemPrompt, java.util.List<java.util.Map<String, Object>> messages) {
        // 取最后一条用户消息
        String userContent = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).get("role"))) {
                userContent = (String) messages.get(i).get("content");
                break;
            }
        }
        return ragQuery(userContent);
    }

    @Override
    public String ragQuery(String question) {
        return ragQuery(question, null);
    }

    @Override
    public String ragQuery(String question, Long courseId) {
        if (agentProperties.getBailianApiKey() == null || agentProperties.getBailianApiKey().isBlank()) {
            throw new IllegalStateException("百炼 API Key 未配置，请在 application.properties 中设置 agent.bailian-api-key");
        }

        // 根据 courseId 路由到对应课程的知识库应用
        String appId = agentProperties.getBailianAppId(courseId);
        log.info("[BailianRAG] courseId={} → appId={}", courseId, appId);

        String requestBody = buildBailianBody(question);

        try {
            String url = agentProperties.getBailianBaseUrl()
                    + "/api/v1/apps/" + appId + "/completion";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + agentProperties.getBailianApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(agentProperties.getLlmTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("百炼 API 返回错误 ({}): {}", response.statusCode(), response.body());
                throw new RuntimeException("百炼 API 返回错误 (" + response.statusCode() + ")");
            }

            return parseBailianReply(response.body());
        } catch (Exception e) {
            log.error("百炼 API 调用失败", e);
            throw new RuntimeException("百炼 API 调用失败: " + e.getMessage(), e);
        }
    }

    private String buildBailianBody(String prompt) {
        return "{\"input\":{\"prompt\":\"" + escape(prompt) + "\"},\"parameters\":{}}";
    }

    private String parseBailianReply(String json) {
        // 百炼返回格式: {"output":{"text":"..."}}
        int idx = json.indexOf("\"text\"");
        if (idx < 0) return "（百炼未返回内容）";
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
