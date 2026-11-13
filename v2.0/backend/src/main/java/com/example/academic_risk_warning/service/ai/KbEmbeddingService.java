package com.example.academic_risk_warning.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** 课程分块 embedding 回填与查询（方案 A）。 */
@Service
@RequiredArgsConstructor
public class KbEmbeddingService {

    private static final tools.jackson.databind.ObjectMapper MAPPER = new tools.jackson.databind.ObjectMapper();
    private final JdbcTemplate jdbc;
    private final EmbeddingClient embedding;

    public Map<String, Object> backfill(boolean force) {        if (!embedding.configured()) throw new IllegalStateException("Embedding 未配置");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select id, course_id, content from ai_kb_chunk order by id");
        int embedded = 0, skipped = 0, failed = 0;
        List<Map<String, Object>> todo = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String hash = hash(String.valueOf(r.get("content")));
            if (!force) {
                Integer n = jdbc.queryForObject(
                        "select count(*) from ai_kb_embedding where chunk_id=? and content_hash=?",
                        Integer.class, ((Number) r.get("id")).longValue(), hash);
                if (n != null && n > 0) { skipped++; continue; }
            }
            r.put("hash", hash);
            todo.add(r);
        }
        for (int i = 0; i < todo.size(); i += 10) {
            List<Map<String, Object>> batch = todo.subList(i, Math.min(todo.size(), i + 10));
            try {
                List<String> texts = batch.stream().map(x -> String.valueOf(x.get("content"))).toList();
                List<double[]> vectors = embedding.embed(texts);
                for (int j = 0; j < batch.size(); j++) {
                    Map<String, Object> r = batch.get(j);
                    jdbc.update("""
                            insert into ai_kb_embedding(chunk_id,course_id,model,dim,vector,content_hash)
                            values(?,?,?,?,?,?)
                            on duplicate key update course_id=values(course_id), model=values(model),
                              dim=values(dim), vector=values(vector), content_hash=values(content_hash)
                            """, ((Number) r.get("id")).longValue(), ((Number) r.get("course_id")).longValue(),
                            embedding.model(), embedding.dim(), MAPPER.writeValueAsString(vectors.get(j)), r.get("hash"));
                    embedded++;
                }
            } catch (Exception e) {
                failed += batch.size();
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", rows.size());
        out.put("embedded", embedded);
        out.put("skipped", skipped);
        out.put("failed", failed);
        return out;
    }

    /** 上传/更新后立即向量化该来源的分块；失败不阻断上传。 */
    public void embedSource(Long courseId, String sourceType, Long sourceId) {
        if (!embedding.configured()) return;
        try {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    select id, course_id, content from ai_kb_chunk
                    where course_id=? and source_type=? and source_id=? order by id
                    """, courseId, sourceType, sourceId);
            for (int i = 0; i < rows.size(); i += 10) {
                List<Map<String, Object>> batch = rows.subList(i, Math.min(rows.size(), i + 10));
                List<double[]> vectors = embedding.embed(batch.stream().map(x -> String.valueOf(x.get("content"))).toList());
                for (int j = 0; j < batch.size(); j++) {
                    Map<String, Object> r = batch.get(j);
                    jdbc.update("""
                            insert into ai_kb_embedding(chunk_id,course_id,model,dim,vector,content_hash)
                            values(?,?,?,?,?,?) on duplicate key update course_id=values(course_id),
                              model=values(model), dim=values(dim), vector=values(vector), content_hash=values(content_hash)
                            """, ((Number) r.get("id")).longValue(), courseId, embedding.model(), embedding.dim(),
                            MAPPER.writeValueAsString(vectors.get(j)), hash(String.valueOf(r.get("content"))));
                }
            }
        } catch (Exception e) {
            // 上传主流程不因 embedding 失败回滚；管理员可手动回填
        }
    }

    public void deleteSource(Long courseId, String sourceType, Long sourceId) {
        jdbc.update("""
                delete e from ai_kb_embedding e join ai_kb_chunk c on c.id=e.chunk_id
                where c.course_id=? and c.source_type=? and c.source_id=?
                """, courseId, sourceType, sourceId);
    }

    private static String hash(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder();
            for (byte x : d) b.append(String.format("%02x", x));
            return b.toString();
        } catch (Exception e) { return String.valueOf(s.hashCode()); }
    }
}
