package com.example.academic_risk_warning.agent.dify;

import com.example.academic_risk_warning.agent.ledger.EvidenceLedger;
import com.example.academic_risk_warning.agent.trace.AgentTraceService;
import com.example.academic_risk_warning.agent.tool.ToolContext;
import com.example.academic_risk_warning.config.AiProperties;
import com.example.academic_risk_warning.service.ai.DifyClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * P3：Diagnostician 的 Dify 第二执行器。
 *
 * <p>约束（与 PLAN §六 一致）：只读 8 工具、输出强制 JSON、复用自研台账/拒答/作用域校验；
 * Dify 失败时由上层决定是否降级本地，并记录 fallback_reason。Dify 未配置或离线时
 * parity 用例标记 skip-if-offline，不判失败。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DifyAgentProvider {

    /** 8 个只读工具：draftIntervention 故意排除，防止外部编排绕过人工确认。 */
    public static final Set<String> READ_ONLY_TOOLS = Set.of(
            "getClassRiskStats", "getRiskTrend", "getCourseCompare", "getStudentRisk",
            "getInterventions", "getStudentMemory", "getAlertExplain", "retrieveCourseDocs");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiProperties props;
    private final DifyClient difyClient;
    private final EvidenceLedger ledger;
    private final AgentTraceService trace;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** Dify 契约不合法：上层可降级本地，并把原因写入 fallback_reason。 */
    public static class DifyContractException extends RuntimeException {
        public DifyContractException(String message) {
            super(message);
        }
    }

    public boolean configured() {
        return props.isEnabled() && props.getDiagnosticianAppKey() != null
                && !props.getDiagnosticianAppKey().isBlank();
    }

    public Map<String, Object> health() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("configured", configured());
        out.put("appKeyPresent", props.getDiagnosticianAppKey() != null && !props.getDiagnosticianAppKey().isBlank());
        out.put("baseUrl", props.getBaseUrl());
        out.put("reachable", difyClient.reachable());
        out.put("allowedTools", READ_ONLY_TOOLS);
        out.put("writeToolExcluded", "draftIntervention");
        out.put("toolTokenPresent", props.getDifyToolToken() != null && !props.getDifyToolToken().isBlank());
        return out;
    }

    /**
     * 调用 Dify Agent App，解析并校验 JSON。
     * 失败抛异常，由 Controller 按 strictDify 决定报错或降级本地。
     */
    public Map<String, Object> diagnose(String question, ToolContext ctx) {
        if (!configured()) {
            throw new DifyContractException("DIFY_NOT_CONFIGURED: 缺少 AI_DIAGNOSTICIAN_APP_KEY");
        }
        long t0 = System.currentTimeMillis();
        AgentTraceService.Start tr = trace.start("AGENT", question, ctx.user(), ctx.termId(), "DIFY");
        Long runId = tr == null ? null : tr.runId();
        String runKey = tr == null ? ledger.newRunKey() : tr.runKey();
        try {
            Map<String, Object> inputs = new LinkedHashMap<>();
            inputs.put("question", question);
            if (ctx.termId() != null) {
                inputs.put("termId", ctx.termId());
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("inputs", inputs);
            body.put("query", question);
            body.put("response_mode", "streaming");
            body.put("conversation_id", "");
            body.put("user", "diagnostician-p3-" + (ctx.user() == null ? "anon" : ctx.user().getAccountId()));

            String url = trimSlash(props.getBaseUrl()) + "/v1/chat-messages";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(5, props.getTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + props.getDiagnosticianAppKey())
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();
            HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            long ms = System.currentTimeMillis() - t0;
            if (resp.statusCode() != 200) {
                String err = new String(resp.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new DifyContractException("DIFY_HTTP_" + resp.statusCode() + ": " + truncate(err, 200));
            }
            String raw = readStreamingAnswer(resp.body());
            if (raw.isBlank()) {
                throw new DifyContractException("DIFY_EMPTY_ANSWER");
            }
            JsonNode contract = parseContract(raw);
            Map<String, Object> normalized = validate(contract, runKey, question);
            normalized.put("provider", "DIFY");
            normalized.put("requestedProvider", "DIFY");
            normalized.put("latencyMs", ms);
            normalized.put("traceRunId", runId);
            if (runId != null) {
                trace.step(runId, 1, "DIFY", "dify-chat", question, raw, "PASS", ms, null);
                trace.finish(runId, "DONE", 1);
            }
            return normalized;
        } catch (Exception e) {
            if (runId != null) {
                trace.step(runId, 1, "DIFY", "dify-chat", question,
                        e.getMessage(), "FAIL", System.currentTimeMillis() - t0, e.getMessage());
                trace.finish(runId, "DEGRADED", 1);
            }
            if (e instanceof DifyContractException d) {
                throw d;
            }
            throw new DifyContractException("DIFY_FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** Agent Chat 只支持 streaming：拼接 agent_message 的 answer，遇 error 立即失败。 */
    private String readStreamingAnswer(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if (payload.isEmpty() || "[DONE]".equals(payload)) {
                    continue;
                }
                JsonNode e;
                try {
                    e = MAPPER.readTree(payload);
                } catch (Exception ignored) {
                    continue;
                }
                String event = e.path("event").asText("");
                if ("error".equals(event)) {
                    throw new DifyContractException("DIFY_STREAM_ERROR: " + e.path("message").asText(""));
                }
                if ("agent_message".equals(event)) {
                    sb.append(e.path("answer").asText(""));
                }
                if ("message_end".equals(event)) {
                    break;
                }
            }
        }
        return sb.toString();
    }

    /** Dify 返回的 answer 必须是 JSON（允许 ```json 围栏/前后解释文字）。 */
    private JsonNode parseContract(String raw) {
        String s = raw.replaceAll("(?s)<think>.*?</think>", "").trim();
        int fence = s.indexOf("```");
        if (fence >= 0) {
            int nl = s.indexOf('\n', fence);
            int end = s.lastIndexOf("```");
            if (nl > 0 && end > nl) {
                s = s.substring(nl + 1, end).trim();
            }
        }
        int l = s.indexOf('{');
        int r = s.lastIndexOf('}');
        if (l < 0 || r <= l) {
            throw new DifyContractException("DIFY_NOT_JSON");
        }
        try {
            return MAPPER.readTree(s.substring(l, r + 1));
        } catch (Exception e) {
            throw new DifyContractException("DIFY_JSON_PARSE: " + e.getMessage());
        }
    }

    private Map<String, Object> validate(JsonNode root, String runKey, String question) {
        if (!root.isObject()) {
            throw new DifyContractException("DIFY_SCHEMA: root not object");
        }
        String answer = root.path("answer").asText("");
        if (answer.isBlank()) {
            throw new DifyContractException("DIFY_SCHEMA: answer empty");
        }
        if (!root.has("refuse") || !root.get("refuse").isBoolean()) {
            throw new DifyContractException("DIFY_SCHEMA: refuse must be boolean");
        }
        boolean refuse = root.get("refuse").booleanValue();
        List<Map<String, Object>> evidence = evidence(root.get("evidence"));
        if (!refuse && evidence.isEmpty()) {
            throw new DifyContractException("DIFY_EVIDENCE: empty evidence for non-refusal answer");
        }
        List<String> tools = tools(root.get("tools"), evidence);
        for (String tool : tools) {
            if (!READ_ONLY_TOOLS.contains(tool)) {
                throw new DifyContractException("DIFY_TOOL_NOT_ALLOWED: " + tool);
            }
        }
        // 同一套台账：Dify 的证据必须入账，答案数字必须能回溯；否则不允许上线。
        int step = 0;
        Map<String, Map<String, Object>> byTool = new LinkedHashMap<>();
        for (Map<String, Object> e : evidence) {
            String tool = String.valueOf(e.get("tool"));
            Map<String, Object> m = byTool.computeIfAbsent(tool, k -> new LinkedHashMap<>());
            m.put(String.valueOf(e.get("metric")), e.get("value"));
            if (e.get("source") != null) {
                m.put("source", e.get("source"));
            }
        }
        for (Map.Entry<String, Map<String, Object>> e : byTool.entrySet()) {
            ledger.record(runKey, ++step, e.getKey(), e.getValue());
        }
        if (!refuse) {
            EvidenceLedger.Validation v = ledger.validate(runKey, answer);
            if (!v.misses().isEmpty()) {
                throw new DifyContractException("DIFY_LEDGER_MISS: " + String.join(",", v.misses()));
            }
        }
        List<Map<String, Object>> actions;
        try {
            actions = root.has("actions") && root.get("actions").isArray()
                    ? MAPPER.convertValue(root.get("actions"), List.class) : List.of();
        } catch (Exception e) {
            actions = List.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("agent", "diagnostician");
        out.put("question", question);
        out.put("intent", root.path("intent").asText(""));
        out.put("tools", tools);
        out.put("evidence", evidence);
        out.put("answer", answer);
        out.put("actions", actions);
        out.put("refused", refuse);
        out.put("provider", "DIFY");
        out.put("requestedProvider", "DIFY");
        out.put("ledger", Map.of("runKey", runKey, "entries", byTool.size()));
        return out;
    }

    private List<Map<String, Object>> evidence(JsonNode node) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return out;
        }
        for (JsonNode e : node) {
            if (!e.isObject() || e.path("tool").asText("").isBlank()
                    || e.path("metric").asText("").isBlank() || !e.has("value")) {
                throw new DifyContractException("DIFY_SCHEMA: evidence item needs tool/metric/value");
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tool", e.get("tool").asText());
            m.put("metric", e.get("metric").asText());
            m.put("value", e.get("value").asText());
            if (e.has("source")) {
                m.put("source", e.get("source").asText());
            }
            out.add(m);
        }
        return out;
    }

    private List<String> tools(JsonNode node, List<Map<String, Object>> evidence) {
        Set<String> set = new LinkedHashSet<>();
        if (node != null && node.isArray()) {
            for (JsonNode t : node) {
                if (!t.asText("").isBlank()) {
                    set.add(t.asText());
                }
            }
        }
        if (set.isEmpty()) {
            for (Map<String, Object> e : evidence) {
                set.add(String.valueOf(e.get("tool")));
            }
        }
        return new ArrayList<>(set);
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
