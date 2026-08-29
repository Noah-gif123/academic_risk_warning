package com.example.academic_risk_warning.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * AI 聊天服务：代理转发到阿里云百炼 Chat API（兼容 OpenAI 格式）
 */
@Service
public class AIChatService {

    @Value("${agent.bailian-api-key:}")
    private String apiKey;

    @Value("${agent.bailian-chat-model:qwen-plus}")
    private String model;

    @Value("${agent.bailian-base-url:https://dashscope.aliyuncs.com}")
    private String baseUrl;

    private static final String SYSTEM_PROMPT =
            "你是阿里云百炼大模型，正在为「学业风险预警系统」提供 AI 助手服务，服务对象为高校教师和管理员。\n" +
            "你的职责：\n" +
            "1. 帮助教师分析学生的学业风险趋势、理解预警机制\n" +
            "2. 为教师提供干预建议和沟通话术\n" +
            "3. 解答关于系统功能（预警管理、题库练习、知识点评估等）的使用问题\n" +
            "4. 协助管理员理解规则配置和数据分析\n\n" +
            "回答要求：简洁实用、条理清晰，用中文回答。如果问题超出职责范围，礼貌说明。";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @SuppressWarnings("unchecked")
    public String chat(Map<String, Object> body) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI 服务未配置 API Key，请在 application.properties 中设置 agent.bailian-api-key");
        }

        // 构建 messages 数组
        List<Map<String, Object>> userMessages;
        if (body.containsKey("messages")) {
            userMessages = (List<Map<String, Object>>) body.get("messages");
        } else {
            String content = (String) body.getOrDefault("content", "");
            userMessages = List.of(Map.of("role", "user", "content", content));
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.addAll(userMessages);

        String requestBody = buildRequestBody(messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/compatible-mode/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("AI API 返回错误 (" + response.statusCode() + "): " + response.body());
        }

        return parseReply(response.body());
    }

    /** 手工构建 JSON 请求体，避免依赖 */
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

    /** 简单解析 reply，取 choices[0].message.content */
    private String parseReply(String json) {
        // 找 "content":" 之后的内容
        int idx = json.indexOf("\"content\"");
        if (idx < 0) return "（AI 未返回内容）";
        // 跳过 "content":
        idx = json.indexOf(":", idx) + 1;
        // 跳过空格和引号
        while (idx < json.length() && (json.charAt(idx) == ' ' || json.charAt(idx) == '"')) idx++;
        // 找结束引号（考虑转义）
        StringBuilder content = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx);
            if (c == '\\') {
                idx++;
                if (idx < json.length()) {
                    char next = json.charAt(idx);
                    if (next == '"') content.append('"');
                    else if (next == '\\') content.append('\\');
                    else if (next == 'n') content.append('\n');
                    else if (next == 't') content.append('\t');
                    else if (next == 'r') content.append('\r');
                    else { content.append('\\'); content.append(next); }
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

    /** JSON 字符串转义 */
    private String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }
}
