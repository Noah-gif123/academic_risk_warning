package com.example.academic_risk_warning.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 方案 A 的本地余弦检索：课程分块做字符 n-gram TF-IDF 向量，查询时本地计算余弦相似度。
 * 不依赖 Dify/Weaviate，不调用 embedding API；课程级小语料下可直接使用。
 */
@Service
@RequiredArgsConstructor
public class LocalCosineRetriever {

    private final JdbcTemplate jdbc;

    public List<RetrievalService.Hit> search(Long courseId, String query, int topK) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select id, doc_name, section_no, source_type, content
                from ai_kb_chunk where course_id = ?
                """, courseId);
        if (rows.isEmpty()) return List.of();
        Map<String, Integer> df = new HashMap<>();
        List<Map<String, Double>> docVectors = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Double> tf = terms(String.valueOf(r.get("content")));
            for (String t : tf.keySet()) df.merge(t, 1, Integer::sum);
            docVectors.add(tf);
        }
        Map<String, Double> qtf = terms(query);
        Map<String, Double> qv = tfidf(qtf, df, rows.size());
        double qnorm = norm(qv);
        if (qnorm == 0) return List.of();

        List<RetrievalService.Hit> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> r = rows.get(i);
            Map<String, Double> dv = tfidf(docVectors.get(i), df, rows.size());
            double dnorm = norm(dv);
            if (dnorm == 0) continue;
            double dot = 0;
            for (Map.Entry<String, Double> e : qv.entrySet()) {
                Double d = dv.get(e.getKey());
                if (d != null) dot += e.getValue() * d;
            }
            double score = dot / (qnorm * dnorm);
            if (score <= 0) continue;
            out.add(new RetrievalService.Hit(
                    ((Number) r.get("id")).longValue(), String.valueOf(r.get("doc_name")),
                    r.get("section_no") == null ? null : ((Number) r.get("section_no")).intValue(),
                    String.valueOf(r.get("source_type")), String.valueOf(r.get("content")),
                    null, null, score, 0, score, 0));
        }
        out.sort((a, b) -> Double.compare(b.vecScore(), a.vecScore()));
        List<RetrievalService.Hit> top = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, out.size()); i++) {
            RetrievalService.Hit h = out.get(i);
            top.add(new RetrievalService.Hit(h.chunkId(), h.docName(), h.sectionNo(), h.sourceType(),
                    h.content(), null, null, h.vecScore(), i + 1, h.vecScore(), i + 1));
        }
        return top;
    }

    private Map<String, Double> terms(String text) {
        String s = text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        Map<String, Double> tf = new HashMap<>();
        for (int i = 0; i + 2 <= s.length(); i++) {
            tf.merge(s.substring(i, i + 2), 1.0, Double::sum);
        }
        return tf;
    }

    private Map<String, Double> tfidf(Map<String, Double> tf, Map<String, Integer> df, int n) {
        Map<String, Double> v = new HashMap<>();
        for (Map.Entry<String, Double> e : tf.entrySet()) {
            double idf = Math.log((n + 1.0) / (df.getOrDefault(e.getKey(), 0) + 1.0)) + 1.0;
            v.put(e.getKey(), (1 + Math.log(e.getValue())) * idf);
        }
        return v;
    }

    private double norm(Map<String, Double> v) {
        double s = 0;
        for (double x : v.values()) s += x * x;
        return Math.sqrt(s);
    }
}
