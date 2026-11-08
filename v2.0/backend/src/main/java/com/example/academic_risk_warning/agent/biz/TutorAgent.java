package com.example.academic_risk_warning.agent.biz;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.service.ai.RetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P2-10 Tutor：带引用的课程答疑契约。
 *
 * <p>复用 RetrievalService 的检索/门控；输出 {answer, citations, refused, gateReason}。
 * 每条引用必须带 chunkId / docName / sectionNo / score，供 P2R-2 的轨迹页定位。</p>
 */
@Component
@RequiredArgsConstructor
public class TutorAgent {

    private final RetrievalService retrieval;
    private final JdbcTemplate jdbc;

    public Map<String, Object> tutor(long courseId, String question) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) {
            throw new BizException(400, "问题不能为空");
        }
        List<String> names = jdbc.queryForList("select course_name from course where id=?", String.class, courseId);
        String courseName = names.isEmpty() ? ("课程" + courseId) : names.get(0);
        RetrievalService.Outcome outcome = retrieval.retrieve(courseId, q, 4);
        RetrievalService.Guard guard = retrieval.guard(outcome, q);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("agent", "tutor");
        out.put("courseId", courseId);
        out.put("courseName", courseName);
        out.put("question", q);
        out.put("retrievalMode", outcome.effective().name());
        List<Map<String, Object>> citations = new ArrayList<>();
        if (guard.refuse()) {
            out.put("refused", true);
            out.put("gateReason", guard.reason());
            out.put("answer", "这个问题在《" + courseName + "》的课程资料中没有足够可靠的依据，暂不给出结论。"
                    + "建议换一个更具体的问法，或到课堂向老师确认。");
            out.put("citations", citations);
            return out;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("根据《").append(courseName).append("》的课程资料：");
        int used = 0;
        for (RetrievalService.Hit h : outcome.hits()) {
            if (used >= 3) {
                break;
            }
            String content = h.content() == null ? "" : h.content().trim();
            if (content.isEmpty()) {
                continue;
            }
            if (used > 0) {
                sb.append("；");
            }
            String snippet = content.length() <= 160 ? content : content.substring(0, 159) + "…";
            sb.append(snippet);
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("chunkId", h.chunkId());
            c.put("docName", h.docName());
            c.put("sectionNo", h.sectionNo());
            c.put("score", h.fused());
            citations.add(c);
            used++;
        }
        sb.append("。以上内容仅来自上述资料片段，具体以教师讲解为准。");
        out.put("refused", false);
        out.put("gateReason", guard.reason());
        out.put("answer", sb.toString());
        out.put("citations", citations);
        out.put("citationCount", citations.size());
        return out;
    }
}
