package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.config.AiProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型直连通道：DashScope 的 OpenAI 兼容接口。
 *
 * <p>定位：**Dify 工作流尚未配好时的等价通道**。上层接口不变（返回正文与 token），
 * 所以"今天用直连、明天切 Dify"对业务代码零改动，这也是选型里"模型可替换"的落地方式。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashScopeClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiProperties props;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Getter
    public static class ChatResult {
        private final boolean ok;
        private final String text;
        private final int totalTokens;
        private final String error;

        ChatResult(boolean ok, String text, int totalTokens, String error) {
            this.ok = ok;
            this.text = text;
            this.totalTokens = totalTokens;
            this.error = error;
        }
    }

    public ChatResult chat(String system, String user) {
        if (!props.dashscopeReady()) {
            return new ChatResult(false, null, 0, "未配置 DashScope Key");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", props.getDashscopeModel());
            body.put("messages", List.of(
                    Map.of("role", "system", "content", system),
                    Map.of("role", "user", "content", user)));
            body.put("temperature", 0.5);
            body.put("max_tokens", 1600);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(trim(props.getDashscopeBaseUrl()) + "/chat/completions"))
                    .timeout(Duration.ofSeconds(Math.max(15, props.getTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + props.getDashscopeKey())
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("[DashScope] HTTP {} body={}", resp.statusCode(), truncate(resp.body(), 300));
                return new ChatResult(false, null, 0, "HTTP " + resp.statusCode());
            }
            JsonNode root = MAPPER.readTree(resp.body());
            String text = root.path("choices").path(0).path("message").path("content").asText("");
            int tokens = root.path("usage").path("total_tokens").asInt(0);
            if (text.isBlank()) {
                return new ChatResult(false, null, tokens, "模型返回空内容");
            }
            return new ChatResult(true, text, tokens, null);
        } catch (Exception e) {
            log.warn("[DashScope] 调用失败: {}", e.getMessage());
            return new ChatResult(false, null, 0, truncate(e.getClass().getSimpleName() + ": " + e.getMessage(), 300));
        }
    }

    private static String trim(String s) {
        if (s == null || s.isBlank()) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
