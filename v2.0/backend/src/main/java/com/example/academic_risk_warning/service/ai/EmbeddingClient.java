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
import java.util.*;

/** OpenAI 兼容 embedding 客户端（方案 A：qwen3.7-text-embedding，1024 维）。 */
@Slf4j
@Component
public class EmbeddingClient {

    private static final tools.jackson.databind.ObjectMapper MAPPER = new tools.jackson.databind.ObjectMapper();
    private final String baseUrl;
    private final String key;
    private final String model;
    private final int dim;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public EmbeddingClient(
            @Value("${ai.embedding.base-url:}") String baseUrl,
            @Value("${ai.embedding.key:}") String key,
            @Value("${ai.embedding.model:qwen3.7-text-embedding}") String model,
            @Value("${ai.embedding.dim:1024}") int dim) {
        this.baseUrl = trim(baseUrl);
        this.key = key == null ? "" : key.trim();
        this.model = model == null ? "qwen3.7-text-embedding" : model.trim();
        this.dim = dim > 0 ? dim : 1024;
    }

    public boolean configured() {
        return !baseUrl.isBlank() && !key.isBlank() && !model.isBlank();
    }

    public int dim() { return dim; }
    public String model() { return model; }

    public List<double[]> embed(List<String> texts) {
        if (!configured()) throw new IllegalStateException("Embedding 未配置");
        if (texts == null || texts.isEmpty()) return List.of();
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += 10) {
            out.addAll(batch(texts.subList(i, Math.min(texts.size(), i + 10))));
        }
        return out;
    }

    public double[] embedOne(String text) {
        List<double[]> v = embed(List.of(text));
        if (v.isEmpty()) throw new IllegalStateException("Embedding 返回为空");
        return v.get(0);
    }

    private List<double[]> batch(List<String> texts) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", texts);
        body.put("dimensions", dim);
        String json;
        try { json = MAPPER.writeValueAsString(body); }
        catch (Exception e) { throw new IllegalStateException("构造 embedding 请求失败", e); }
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/embeddings"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() / 100 != 2) throw new IllegalStateException("Embedding HTTP " + res.statusCode());
            tools.jackson.databind.JsonNode root = MAPPER.readTree(res.body());
            Map<Integer, double[]> ordered = new TreeMap<>();
            for (tools.jackson.databind.JsonNode d : root.path("data")) {
                int idx = d.path("index").asInt(ordered.size());
                tools.jackson.databind.JsonNode arr = d.path("embedding");
                double[] v = new double[arr.size()];
                for (int i = 0; i < arr.size(); i++) v[i] = arr.get(i).asDouble();
                ordered.put(idx, v);
            }
            List<double[]> out = new ArrayList<>(ordered.values());
            if (out.size() != texts.size()) throw new IllegalStateException("Embedding 返回数量不匹配");
            return out;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Embedding 被中断", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Embedding 调用失败：" + e.getMessage(), e);
        }
    }

    private static String trim(String s) {
        if (s == null) return "";
        String t = s.trim();
        return t.endsWith("/") ? t.substring(0, t.length() - 1) : t;
    }
}
