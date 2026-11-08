package com.example.academic_risk_warning.agent.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P2R-2 引用绑定：把工具返回的命中绑定到可核查的 chunk 原文。
 *
 * <p>每条引用必须带 chunkId；校验 chunk 存在并返回 docId/docName/sectionNo/score/content。
 * Writer/Tutor 的引用经此绑定后，轨迹页可点开原文片段。</p>
 */
@Service
@RequiredArgsConstructor
public class CitationBinder {

    private final JdbcTemplate jdbc;

    public record Citation(Long chunkId, Long docId, String docName, Integer sectionNo,
                           Double score, Integer rrfRank, String content) {
    }

    public record BindResult(List<Citation> citations, boolean valid, List<Long> missing) {
    }

    public BindResult bind(List<Map<String, Object>> hits) {
        List<Citation> citations = new ArrayList<>();
        List<Long> missing = new ArrayList<>();
        for (Map<String, Object> h : hits) {
            Object cid = h.get("chunkId");
            if (cid == null) {
                missing.add(null);
                continue;
            }
            long chunkId = ((Number) cid).longValue();
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "select id, course_id courseId, doc_name docName, section_no sectionNo, content"
                            + " from ai_kb_chunk where id=?", chunkId);
            if (rows.isEmpty()) {
                missing.add(chunkId);
                continue;
            }
            Map<String, Object> row = rows.get(0);
            String content = String.valueOf(row.get("content"));
            citations.add(new Citation(chunkId,
                    h.get("docId") == null ? null : ((Number) h.get("docId")).longValue(),
                    String.valueOf(row.get("docName")),
                    row.get("sectionNo") == null ? null : ((Number) row.get("sectionNo")).intValue(),
                    h.get("score") == null ? null : ((Number) h.get("score")).doubleValue(),
                    h.get("rrfRank") == null ? null : ((Number) h.get("rrfRank")).intValue(),
                    content.length() <= 600 ? content : content.substring(0, 599) + "…"));
        }
        return new BindResult(citations, missing.isEmpty(), missing);
    }

    public Map<String, Object> chunk(long chunkId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select id chunkId, course_id courseId, doc_name docName, section_no sectionNo, content"
                        + " from ai_kb_chunk where id=?", chunkId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> citationMap(Citation c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("chunkId", c.chunkId());
        m.put("docId", c.docId());
        m.put("docName", c.docName());
        m.put("sectionNo", c.sectionNo());
        m.put("score", c.score());
        m.put("rrfRank", c.rrfRank());
        return m;
    }
}
