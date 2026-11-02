package com.example.academic_risk_warning.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dify 知识库的向量检索客户端（R2 的「向量路」）。
 *
 * <p>分工约定：<b>Dify 只做向量底座</b>（文档解析 → 分块 → embedding → Weaviate 存储与召回），
 * 本系统自己实现双路融合、重排、阈值、拒答与评测。调的是 Dify 的知识库检索接口
 * {@code POST /v1/datasets/{dataset_id}/retrieve}，用的是 <b>知识库 API Key</b>
 * （控制台「知识库 → API」里创建），与应用 App Key 不是一回事。</p>
 *
 * <p>没配 key 时不报错、不阻塞：{@link #configured()} 返回 false，检索层自动只用全文路，
 * 降级原因会写进诊断信息，便于在 {@code ai_qa_log.retrieval_json} 里看出「向量路为什么没参与」。</p>
 */
@Slf4j
@Component
public class DifyVectorClient {

    private static final tools.jackson.databind.ObjectMapper MAPPER =
            new tools.jackson.databind.ObjectMapper();

    private final String baseUrl;
    private final String datasetKey;
    private final String datasetId;
    private final int topK;
    private final boolean filterByCourse;
    private final int timeoutSeconds;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public DifyVectorClient(
            @Value("${app.ai.base-url:http://localhost:8090}") String baseUrl,
            @Value("${app.ai.retrieval.dataset-key:}") String datasetKey,
            @Value("${app.ai.retrieval.dataset-id:}") String datasetId,
            @Value("${app.ai.retrieval.vector-top-k:20}") int topK,
            @Value("${app.ai.retrieval.filter-by-course:true}") boolean filterByCourse,
            @Value("${app.ai.timeout-seconds:120}") int timeoutSeconds) {
        this.baseUrl = trimSlash(baseUrl);
        this.datasetKey = datasetKey == null ? "" : datasetKey.trim();
        this.datasetId = datasetId == null ? "" : datasetId.trim();
        this.topK = Math.max(1, Math.min(topK, 50));
        this.filterByCourse = filterByCourse;
        this.timeoutSeconds = timeoutSeconds;
    }

    /** 向量路是否可用（配了知识库 key + dataset id 才算） */
    public boolean configured() {
        return !datasetKey.isBlank() && !datasetId.isBlank();
    }

    /** 没配置时的原因，供诊断信息使用 */
    public String notConfiguredReason() {
        if (datasetId.isBlank()) {
            return "向量通道未配置：缺 AI_RAG_DATASET_ID（Dify 知识库 ID）";
        }
        return "向量通道未配置：缺 AI_RAG_DATASET_KEY（Dify 知识库 API Key）";
    }

    /**
     * 向量召回。失败抛异常，由调用方决定降级（保持"向量路挂了全文路照常"）。
     *
     * @param courseId 课程 ID；开启 filterByCourse 时按 metadata.course_id 过滤
     */
    public List<VecHit> search(Long courseId, String query) {
        if (!configured()) {
            throw new IllegalStateException(notConfiguredReason());
        }
        String body = buildBody(courseId, query);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/datasets/" + datasetId + "/retrieve"))
                .timeout(Duration.ofSeconds(Math.max(5, Math.min(timeoutSeconds, 60))))
                .header("Authorization", "Bearer " + datasetKey)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() / 100 != 2) {
                throw new IllegalStateException("知识库检索 HTTP " + res.statusCode() + "：" + brief(res.body()));
            }
            return parse(res.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("知识库检索被中断", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("知识库检索调用失败：" + e.getMessage(), e);
        }
    }

    private String buildBody(Long courseId, String query) {
        Map<String, Object> retrieval = new LinkedHashMap<>();
        // hybrid_search = Dify 内部先用「语义 + 关键词」各召回再合并，最接近我们要对比的"向量路"能力
        retrieval.put("search_method", "hybrid_search");
        retrieval.put("reranking_enable", false);
        retrieval.put("top_k", topK);
        retrieval.put("score_threshold_enabled", false);
        retrieval.put("score_threshold", 0.0);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("retrieval_model", retrieval);
        if (filterByCourse && courseId != null) {
            Map<String, Object> cond = new LinkedHashMap<>();
            cond.put("name", "course_id");
            cond.put("comparison_operator", "is");
            cond.put("value", String.valueOf(courseId));
            cond.put("logical_operator", "and");
            retrieval.put("metadata_filtering_conditions", List.of(cond));
        }
        try {
            return MAPPER.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("构造知识库检索请求失败", e);
        }
    }

    private List<VecHit> parse(String json) {
        List<VecHit> out = new ArrayList<>();
        tools.jackson.databind.JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("知识库检索响应不是合法 JSON", e);
        }
        int rank = 0;
        for (tools.jackson.databind.JsonNode rec : root.path("records")) {
            rank++;
            tools.jackson.databind.JsonNode seg = rec.path("segment");
            String content = seg.path("content").asText("");
            String docName = seg.path("document").path("name").asText("");
            Integer position = seg.hasNonNull("position") ? seg.path("position").asInt() : null;
            String segId = seg.hasNonNull("id") ? seg.path("id").asText() : null;
            out.add(new VecHit(segId, docName, position, content, rec.path("score").asDouble(0d), rank));
        }
        return out;
    }

    private static String brief(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 200 ? s : s.substring(0, 200) + "…";
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.endsWith("/") ? t.substring(0, t.length() - 1) : t;
    }

    /** 向量路的一条命中（Dify segment 粒度） */
    public record VecHit(String segmentId, String docName, Integer position, String content,
                         double score, int rank) {
    }
}
