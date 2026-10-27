package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.config.AiProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dify Service API 客户端（只做三件事：跑工作流 / 探活 / 解析用量）。
 *
 * <p>设计约束：</p>
 * <ul>
 *   <li>API Key 只在后端，前端永不直连 Dify；</li>
 *   <li>任何异常都向上抛 {@link IllegalStateException}，由调用方降级到模板，不让页面报 500；</li>
 *   <li>不缓存、不重试（重试在上层按业务判断），避免一次请求打两次钱。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DifyClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiProperties props;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** 工作流结果 */
    @Getter
    public static class WorkflowResult {
        private final boolean ok;
        private final Map<String, Object> outputs;
        private final int totalTokens;
        private final String error;

        WorkflowResult(boolean ok, Map<String, Object> outputs, int totalTokens, String error) {
            this.ok = ok;
            this.outputs = outputs;
            this.totalTokens = totalTokens;
            this.error = error;
        }
    }

    /** 跑一次 workflow（blocking） */
    public WorkflowResult runWorkflow(String appKey, Map<String, Object> inputs, String user) {
        if (appKey == null || appKey.isBlank()) {
            return new WorkflowResult(false, Map.of(), 0, "未配置 App API Key");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("inputs", inputs == null ? Map.of() : inputs);
            body.put("response_mode", "blocking");
            body.put("user", user == null ? "system" : user);

            String url = trimSlash(props.getBaseUrl()) + "/v1/workflows/run";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(10, props.getTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + appKey)
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("[Dify] workflow 调用失败 status={} body={}", resp.statusCode(), truncate(resp.body(), 300));
                return new WorkflowResult(false, Map.of(), 0, "HTTP " + resp.statusCode());
            }

            JsonNode root = MAPPER.readTree(resp.body());
            JsonNode data = root.path("data");
            String status = data.path("status").asText("");
            int tokens = data.path("total_tokens").asInt(0);
            JsonNode outputsNode = data.path("outputs");
            Map<String, Object> outputs = new LinkedHashMap<>();
            if (outputsNode.isObject()) {
                outputsNode.properties().forEach(e ->
                        outputs.put(e.getKey(), e.getValue().isValueNode() ? e.getValue().asText() : e.getValue().toString()));
            }
            if (!"succeeded".equals(status)) {
                String err = data.path("error").asText("工作流未成功：" + status);
                log.warn("[Dify] workflow status={} error={}", status, err);
                return new WorkflowResult(false, outputs, tokens, truncate(err, 300));
            }
            return new WorkflowResult(true, outputs, tokens, null);
        } catch (Exception e) {
            log.warn("[Dify] workflow 异常: {}", e.getMessage());
            return new WorkflowResult(false, Map.of(), 0, truncate(e.getClass().getSimpleName() + ": " + e.getMessage(), 300));
        }
    }

    /** 探活：能连上 Dify 控制台即认为可达（不消耗 token） */
    public boolean reachable() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(trimSlash(props.getBaseUrl()) + "/console/api/setup"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<Void> resp = http.send(req, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String trimSlash(String s) {
        if (s == null || s.isBlank()) {
            return "http://localhost:8090";
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
