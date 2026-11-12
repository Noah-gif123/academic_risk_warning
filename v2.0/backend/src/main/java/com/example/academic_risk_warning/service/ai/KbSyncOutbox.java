package com.example.academic_risk_warning.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** 本地知识库 → Dify 向量副本的同步任务记账；实际同步 worker 由后续阶段消费。 */
@Service
@RequiredArgsConstructor
public class KbSyncOutbox {

    private final JdbcTemplate jdbc;

    public void enqueue(Long courseId, Long docId, String action) {
        if (courseId == null) return;
        jdbc.update("insert into ai_kb_sync_outbox(course_id, doc_id, action, status) values (?,?,?, 'PENDING')",
                courseId, docId, action);
    }

    public int pendingCount() {
        Integer n = jdbc.queryForObject("select count(*) from ai_kb_sync_outbox where status = 'PENDING'", Integer.class);
        return n == null ? 0 : n;
    }
}
