package com.example.academic_risk_warning.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/** 方案 A 主检索：本地 embedding + 余弦相似度。 */
@Service
@RequiredArgsConstructor
public class EmbeddingCosineRetriever {

    private static final tools.jackson.databind.ObjectMapper MAPPER = new tools.jackson.databind.ObjectMapper();
    private final JdbcTemplate jdbc;
    private final EmbeddingClient embedding;

    public List<RetrievalService.Hit> search(Long courseId, String query, int topK) {
        if (!embedding.configured()) return List.of();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select e.chunk_id,e.vector,c.doc_name,c.section_no,c.source_type,c.content
                from ai_kb_embedding e join ai_kb_chunk c on c.id=e.chunk_id
                where e.course_id=? and e.model=?
                """, courseId, embedding.model());
        if (rows.isEmpty()) return List.of();
        double[] q = embedding.embedOne(query);
        double qn = norm(q);
        if (qn == 0) return List.of();
        List<RetrievalService.Hit> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            try {
                double[] v = MAPPER.readValue(String.valueOf(r.get("vector")), double[].class);
                double dn = norm(v);
                if (dn == 0 || v.length != q.length) continue;
                double dot = 0;
                for (int i = 0; i < v.length; i++) dot += q[i] * v[i];
                double score = dot / (qn * dn);
                if (score <= 0) continue;
                out.add(new RetrievalService.Hit(
                        ((Number) r.get("chunk_id")).longValue(), String.valueOf(r.get("doc_name")),
                        r.get("section_no") == null ? null : ((Number) r.get("section_no")).intValue(),
                        String.valueOf(r.get("source_type")), String.valueOf(r.get("content")),
                        null, null, score, 0, score, 0));
            } catch (Exception ignored) { }
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

    private double norm(double[] v) {
        double s = 0;
        for (double x : v) s += x * x;
        return Math.sqrt(s);
    }
}
