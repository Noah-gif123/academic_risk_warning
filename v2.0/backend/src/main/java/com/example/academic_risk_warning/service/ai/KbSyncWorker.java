package com.example.academic_risk_warning.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 消费本地知识库 outbox，把文档同步到 Dify 向量库。失败留在 PENDING 并记录 error。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KbSyncWorker {

    private final JdbcTemplate jdbc;
    private final DifyVectorClient dify;

    @Scheduled(fixedDelay = 60_000, initialDelay = 20_000)
    public void drain() {
        if (!dify.configured()) return;
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select id, course_id, doc_id, action from ai_kb_sync_outbox where status='PENDING' order by id limit 10");
        for (Map<String, Object> r : rows) {
            long outboxId = ((Number) r.get("id")).longValue();
            long courseId = ((Number) r.get("course_id")).longValue();
            Long docId = r.get("doc_id") == null ? null : ((Number) r.get("doc_id")).longValue();
            String action = String.valueOf(r.get("action"));
            try {
                if (docId == null) {
                    mark(outboxId, "SKIPPED", null);
                    continue;
                }
                List<Map<String, Object>> docs = jdbc.queryForList(
                        "select id, doc_name, source_type, source_id, dify_document_id from ai_kb_doc where id=?", docId);
                if (docs.isEmpty()) {
                    mark(outboxId, "SKIPPED", "本地文档已删除");
                    continue;
                }
                Map<String, Object> doc = docs.get(0);
                String difyId = doc.get("dify_document_id") == null ? null : String.valueOf(doc.get("dify_document_id"));
                if ("DELETE".equals(action)) {
                    if (difyId != null) dify.deleteDocument(difyId);
                    mark(outboxId, "SUCCESS", null);
                    continue;
                }
                String text = jdbc.queryForObject("""
                        select coalesce(group_concat(content order by section_no separator '\\n'), '')
                        from ai_kb_chunk where course_id=? and source_type=? and source_id=?
                        """, String.class, courseId, doc.get("source_type"), doc.get("source_id"));
                if (text == null || text.isBlank()) {
                    mark(outboxId, "SKIPPED", "没有可同步的分块");
                    continue;
                }
                if (difyId != null) dify.deleteDocument(difyId);
                String newId = dify.createDocument(courseId, String.valueOf(doc.get("doc_name")), text);
                jdbc.update("update ai_kb_doc set dify_document_id=? where id=?", newId, docId);
                mark(outboxId, "SUCCESS", null);
            } catch (Exception e) {
                log.warn("[KB-SYNC] outbox={} 失败: {}", outboxId, e.getMessage());
                jdbc.update("update ai_kb_sync_outbox set status='PENDING', attempts=attempts+1, error=? where id=?",
                        e.getMessage() == null ? "unknown" : e.getMessage().substring(0, Math.min(480, e.getMessage().length())),
                        outboxId);
            }
        }
    }

    private void mark(long id, String status, String error) {
        jdbc.update("update ai_kb_sync_outbox set status=?, error=? where id=?", status, error, id);
    }
}
