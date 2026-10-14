package com.example.academic_risk_warning.llm;

import com.example.academic_risk_warning.agent.core.JsonUtils;
import com.example.academic_risk_warning.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        return ragQueryDetailed(question, courseId).text();
    }

    /**
     * RAG 回答 + 引用/依据（W3）：
     * <ul>
     *   <li>{@code citations}：优先取应用返回的 {@code output.doc_references}（结构化引用）；
     *       应用未开启引用返回时，退化为从 {@code output.thoughts} 里提取的检索工具命中（文件名/关键词）；</li>
     *   <li>{@code refusal}：回答属于"超出课程范围/知识库未检索到"的拒答；</li>
     *   <li>{@code grounded}：是否有可追溯的引用依据。</li>
     * </ul>
     */
    public RagAnswer ragQueryDetailed(String question, Long courseId) {
        if (!agentProperties.isEnabled()) {
            throw new IllegalStateException("多智能体功能已关闭（agent.enabled=false）");
        }
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
                    .timeout(Duration.ofSeconds(effectiveTimeoutSeconds()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("百炼 API 返回错误 ({}): {}", response.statusCode(), response.body());
                throw new RuntimeException("百炼 API 返回错误 (" + response.statusCode() + ")");
            }

            return parseRagAnswer(response.body());
        } catch (Exception e) {
            log.error("百炼 API 调用失败", e);
            throw new RuntimeException("百炼 API 调用失败: " + e.getMessage(), e);
        }
    }

    /** RAG 回答结构化结果 */
    public record RagAnswer(String text, List<Map<String, Object>> citations,
                            boolean grounded, boolean refusal, boolean retrievalEmpty) {}

    /** 从回答里拆出的"助手声明的依据"（正文 + 依据名称清单） */
    public record DeclaredEvidence(String text, List<String> sources) {}

    /**
     * 解析回答末尾的「依据：xxx」声明（百炼应用未返回结构化引用时的兜底溯源手段），
     * 并把该行从正文中剥离，避免重复展示。
     */
    public static DeclaredEvidence splitDeclaredEvidence(String text) {
        if (text == null || text.isBlank()) {
            return new DeclaredEvidence(text == null ? "" : text, List.of());
        }
        List<String> sources = new ArrayList<>();
        StringBuilder kept = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            String trimmed = line.trim();
            if (isEvidenceLine(trimmed)) {
                String body = trimmed.substring(trimmed.indexOf('：') >= 0 ? trimmed.indexOf('：') + 1 : trimmed.indexOf(':') + 1);
                for (String part : body.split("[、,，;；]")) {
                    String name = part.replaceAll("[*《》\\[\\]【】]", "").trim();
                    if (!name.isBlank() && !sources.contains(name)) {
                        sources.add(name.length() <= 80 ? name : name.substring(0, 79) + "…");
                    }
                }
                continue;
            }
            kept.append(line).append('\n');
        }
        String cleaned = kept.toString().trim();
        return new DeclaredEvidence(cleaned.isEmpty() ? text : cleaned, sources.stream().limit(5).toList());
    }

    private static boolean isEvidenceLine(String line) {
        if (line.isEmpty() || line.length() > 200) return false;
        String normalized = line.replace("*", "").replace(" ", "");
        return normalized.startsWith("依据：") || normalized.startsWith("依据:")
                || normalized.startsWith("参考来源：") || normalized.startsWith("参考资料：")
                || normalized.startsWith("来源：");
    }


    /** 兼容旧调用：无 retrievalEmpty 信息时按"未检索"处理 */
    public static RagAnswer of(String text, List<Map<String, Object>> citations, boolean grounded, boolean refusal) {
        return new RagAnswer(text, citations, grounded, refusal, false);
    }

    /** 无依据时统一给出的拒答话术（不编造） */
    public static final String NO_EVIDENCE_REFUSAL =
            "这个问题我在本课程知识库里没有检索到可靠依据，为了避免给你错误信息，我先不作答。"
            + "建议你换个问法，或直接找任课老师确认。";


    /** 单次调用超时：以 agent.llm-timeout 为准，同时不超过 agent.agent-timeout */
    private long effectiveTimeoutSeconds() {
        int llm = agentProperties.getLlmTimeout();
        int agent = agentProperties.getAgentTimeout();
        int seconds = agent > 0 ? Math.min(llm, agent) : llm;
        return Math.max(1, seconds);
    }

    private String buildBailianBody(String prompt) {
        // has_thoughts=true 时应用会返回内部检索过程，便于提取"引用来源"（W3）
        String parameters = agentProperties.isRagThoughtsEnabled() ? "{\"has_thoughts\":true}" : "{}";
        return "{\"input\":{\"prompt\":\"" + escape(prompt) + "\"},\"parameters\":" + parameters + "}";
    }

    // ==================== W3：引用与依据解析 ====================

    /** 判定"知识库无依据"的拒答话术特征 */
    private static final List<String> REFUSAL_MARKERS = List.of(
            "超出当前课程范围", "超出课程范围", "课程范围", "不在本课程",
            "未找到", "没有找到", "未检索到", "没有检索到", "未提供", "知识库中未", "知识库未",
            "无法回答这个问题", "无法确定");

    /** 引用字段名（应用返回 doc_references 时） */
    private static final List<String> CITATION_KEYS = List.of(
            "title", "doc_name", "docName", "file_name", "fileName", "name");

    List<Map<String, Object>> extractCitations(Map<String, Object> output) {
        List<Map<String, Object>> citations = new ArrayList<>();

        Object docs = output.get("doc_references");
        if (docs instanceof List<?> list) {
            int index = 1;
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> raw)) continue;
                Map<String, Object> citation = new LinkedHashMap<>();
                citation.put("index", index++);
                citation.put("source", "DOC_REFERENCE");
                for (String key : CITATION_KEYS) {
                    Object v = raw.get(key);
                    if (v != null && !String.valueOf(v).isBlank()) {
                        citation.put("title", String.valueOf(v));
                        break;
                    }
                }
                Object snippet = raw.get("text") != null ? raw.get("text") : raw.get("content");
                if (snippet != null) {
                    citation.put("snippet", truncate(String.valueOf(snippet).replaceAll("\\s+", " ").trim(), 120));
                }
                if (raw.get("score") != null) {
                    citation.put("score", raw.get("score"));
                }
                citations.add(citation);
            }
        }

        // 应用未返回结构化引用时，从思考过程里的"检索工具调用"提取命中来源
        if (citations.isEmpty() && agentProperties.isRagThoughtsEnabled()) {
            collectToolCallCitations(output.get("thoughts"), citations);
        }
        return citations;
    }

    /**
     * 从思考过程中的 MCP 工具调用（如 searchFile/getFile）提取引用：
     * 参数里的关键词/文件名作为"检索关键词"，observation 里的命中内容作为片段；
     * observation 明确为空（"查询结果为空"）时不算引用。
     */
    private void collectToolCallCitations(Object thoughts, List<Map<String, Object>> citations) {
        if (!(thoughts instanceof List<?> list)) return;
        Set<String> seen = new LinkedHashSet<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) continue;
            String actionType = str(raw.get("action_type"));
            String action = str(raw.get("action"));
            boolean looksLikeRetrieval = "mcp".equalsIgnoreCase(actionType)
                    || (action != null && (action.toLowerCase().contains("search") || action.toLowerCase().contains("getfile")
                    || action.toLowerCase().contains("retriev") || action.toLowerCase().contains("knowledge")));
            if (!looksLikeRetrieval) continue;

            Map<String, Object> args = parseJsonObject(raw.get("arguments") != null ? raw.get("arguments") : raw.get("action_input_stream"));
            String keyword = firstNonBlank(args, "keyWord", "keyword", "query", "fileName", "docName", "title");
            String observation = observationText(raw.get("observation"));
            boolean empty = observation.isBlank() || observation.contains("查询结果为空")
                    || observation.contains("未找到") || observation.contains("结果为空");

            if (empty || keyword == null) {
                continue;
            }
            String title = "知识库检索命中：" + keyword;
            if (!seen.add(title)) continue;
            Map<String, Object> citation = new LinkedHashMap<>();
            citation.put("index", citations.size() + 1);
            citation.put("source", "APP_RETRIEVAL");
            citation.put("title", title);
            citation.put("snippet", truncate(observation.replaceAll("\\s+", " ").trim(), 120));
            citations.add(citation);
            if (citations.size() >= 5) return;
        }
    }

    /** 判断本次回答是否"检索了但一无所获" */
    private boolean isRetrievalEmpty(Object thoughts) {
        if (!(thoughts instanceof List<?> list)) return false;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) continue;
            String observation = observationText(raw.get("observation"));
            if (!observation.isBlank() && (observation.contains("查询结果为空") || observation.contains("结果为空"))) {
                return true;
            }
        }
        return false;
    }

    /** observation 可能是 JSON 字符串（content[].text）或纯文本 */
    private String observationText(Object observation) {
        if (observation == null) return "";
        String raw = String.valueOf(observation);
        if (!raw.trim().startsWith("{")) return raw;
        Map<String, Object> parsed = JsonUtils.parse(raw);
        StringBuilder sb = new StringBuilder();
        Object content = parsed.get("content");
        if (content instanceof List<?> items) {
            for (Object it : items) {
                if (it instanceof Map<?, ?> m && m.get("text") != null) {
                    sb.append(m.get("text")).append(' ');
                }
            }
        }
        if (sb.length() == 0) {
            sb.append(String.valueOf(parsed.getOrDefault("text", ""))).append(' ')
              .append(String.valueOf(parsed.getOrDefault("structuredContent", "")));
        }
        return sb.toString().trim();
    }

    private Map<String, Object> parseJsonObject(Object value) {
        if (value == null) return Map.of();
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        String text = String.valueOf(value).trim();
        if (!text.startsWith("{")) return Map.of();
        Map<String, Object> parsed = JsonUtils.parse(text);
        return parsed.get("raw") != null ? Map.of() : parsed;
    }

    private String firstNonBlank(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v != null && !String.valueOf(v).isBlank()) {
                return truncate(String.valueOf(v).trim(), 60);
            }
        }
        return null;
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    RagAnswer parseRagAnswer(String json) {
        Map<String, Object> root = JsonUtils.parse(json);
        Object outputRaw = root.get("output");
        if (!(outputRaw instanceof Map<?, ?> outputMap)) {
            return new RagAnswer(parseBailianReply(json), List.of(), false, false, false);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) outputMap;

        String text = output.get("text") == null ? "（百炼未返回内容）" : String.valueOf(output.get("text"));
        List<Map<String, Object>> citations = extractCitations(output);
        boolean retrievalEmpty = citations.isEmpty() && isRetrievalEmpty(output.get("thoughts"));
        boolean refusal = isRefusal(text) || (retrievalEmpty && text.length() < 200);
        log.info("[BailianRAG] citations={} thoughts={} retrievalEmpty={} refusal={}",
                citations.size(), thoughtsSize(output.get("thoughts")), retrievalEmpty, refusal);
        return new RagAnswer(text, citations, !citations.isEmpty(), refusal, retrievalEmpty);
    }

    private int thoughtsSize(Object thoughts) {
        return thoughts instanceof List<?> list ? list.size() : 0;
    }

    boolean isRefusal(String text) {
        if (text == null || text.isBlank()) return true;
        for (String marker : REFUSAL_MARKERS) {
            if (text.contains(marker)) return true;
        }
        return false;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
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
