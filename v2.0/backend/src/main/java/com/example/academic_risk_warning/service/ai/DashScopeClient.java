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
import java.util.function.Consumer;

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
        /** 是否被 max_tokens 截断（finish_reason=length）：群体报告遇到短内容要重试 */
        private final boolean truncated;

        ChatResult(boolean ok, String text, int totalTokens, String error) {
            this(ok, text, totalTokens, error, false);
        }

        ChatResult(boolean ok, String text, int totalTokens, String error, boolean truncated) {
            this.ok = ok;
            this.text = text;
            this.totalTokens = totalTokens;
            this.error = error;
            this.truncated = truncated;
        }
    }

    public ChatResult chat(String system, String user) {
        return chat(system, user, 0.5, 2400);
    }

    /**
     * 可调温度与最大输出长度（出题需要更多 token、报告需要更稳的语气）。
     *
     * <p>推理型模型（如 deepseek-flash）会先花 completion 预算做 reasoning：提示一长就可能把预算烧光、
     * 返回空 content（finish_reason=length）。这里遇到这种情况自动把预算翻倍重试一次，
     * 仍不行再如实报错，避免"白花 token 又退回模板"。</p>
     */
    public ChatResult chat(String system, String user, double temperature, int maxTokens) {
        ChatResult first = once(system, user, temperature, maxTokens);
        // 两种要重试的情况：
        //   1) 预算被 reasoning 燒光、content 完全为空；
        //   2) content 非空但被截断得很短（群体报告要求四段，几十个字的残句等于废稿）。
        boolean burned = !first.isOk() && first.getError() != null && first.getError().contains("finish_reason=length");
        boolean halfDone = first.isOk() && first.isTruncated()
                && (first.getText() == null || first.getText().trim().length() < 200);
        if (burned || halfDone) {
            int retryBudget = Math.min(8000, Math.max(maxTokens, 1600) * 2);
            log.info("[DashScope] 输出被长度截断（{}），按 max_tokens={} 重试一次",
                    burned ? "空内容" : "残句", retryBudget);
            return once(system, user, temperature, retryBudget);
        }
        return first;
    }

    /** 真 token 级流式：每收到一个 delta 回调一次，结束后返回完整文本。 */
    public ChatResult streamChat(String system, String user, Consumer<String> onDelta) {
        if (!props.directReady()) {
            return new ChatResult(false, null, 0, "未配置 DashScope Key");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", props.directModel());
            body.put("messages", List.of(
                    Map.of("role", "system", "content", system),
                    Map.of("role", "user", "content", user)));
            body.put("temperature", 0.5);
            body.put("max_tokens", 2400);
            body.put("stream", true);
            body.put("stream_options", Map.of("include_usage", true));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(trim(props.directBaseUrl()) + "/chat/completions"))
                    .timeout(Duration.ofSeconds(Math.max(15, props.getTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + props.directApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();
            HttpResponse<java.util.stream.Stream<String>> resp =
                    http.send(req, HttpResponse.BodyHandlers.ofLines());
            if (resp.statusCode() != 200) {
                return new ChatResult(false, null, 0, "HTTP " + resp.statusCode());
            }
            StringBuilder full = new StringBuilder();
            int[] tokens = {0};
            resp.body().forEach(line -> {
                String s = line == null ? "" : line.trim();
                if (!s.startsWith("data:")) return;
                String data = s.substring(5).trim();
                if ("[DONE]".equals(data)) return;
                try {
                    JsonNode root = MAPPER.readTree(data);
                    JsonNode choice = root.path("choices").path(0);
                    String delta = choice.path("delta").path("content").asText("");
                    if (!delta.isEmpty()) {
                        full.append(delta);
                        onDelta.accept(delta);
                    }
                    if (root.has("usage")) {
                        tokens[0] = root.path("usage").path("total_tokens").asInt(tokens[0]);
                    }
                } catch (Exception ignored) { }
            });
            String text = full.toString();
            if (text.isBlank()) return new ChatResult(false, null, tokens[0], "流式返回空内容");
            return new ChatResult(true, text, tokens[0], null);
        } catch (Exception e) {
            log.warn("[DashScope] 流式调用失败: {}", e.getMessage());
            return new ChatResult(false, null, 0, truncate(e.getClass().getSimpleName() + ": " + e.getMessage(), 300));
        }
    }

    private ChatResult once(String system, String user, double temperature, int maxTokens) {
        if (!props.directReady()) {
            return new ChatResult(false, null, 0, "未配置 DashScope Key");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", props.directModel());
            body.put("messages", List.of(
                    Map.of("role", "system", "content", system),
                    Map.of("role", "user", "content", user)));
            body.put("temperature", temperature);
            body.put("max_tokens", maxTokens);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(trim(props.directBaseUrl()) + "/chat/completions"))
                    .timeout(Duration.ofSeconds(Math.max(15, props.getTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + props.directApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("[DashScope] HTTP {} body={}", resp.statusCode(), truncate(resp.body(), 300));
                return new ChatResult(false, null, 0, "HTTP " + resp.statusCode());
            }
            JsonNode root = MAPPER.readTree(resp.body());
            String text = root.path("choices").path(0).path("message").path("content").asText("");
            String finish = root.path("choices").path(0).path("finish_reason").asText("");
            int tokens = root.path("usage").path("total_tokens").asInt(0);
            int reasoningTokens = root.path("usage").path("completion_tokens_details")
                    .path("reasoning_tokens").asInt(0);
            if (text.isBlank()) {
                return new ChatResult(false, null, tokens,
                        "模型返回空内容(finish_reason=" + finish + ", reasoning_tokens=" + reasoningTokens
                                + ", max_tokens=" + maxTokens + ")");
            }
            if ("length".equals(finish)) {
                // 有内容但被截断：先用着，但留痕方便排查
                log.warn("[DashScope] 输出被截断(finish_reason=length)，tokens={}", tokens);
            }
            return new ChatResult(true, text, tokens, null, "length".equals(finish));
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
