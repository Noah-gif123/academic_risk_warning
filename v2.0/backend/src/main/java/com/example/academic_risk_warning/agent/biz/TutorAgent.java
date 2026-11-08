package com.example.academic_risk_warning.agent.biz;

import com.example.academic_risk_warning.agent.rag.CitationBinder;
import com.example.academic_risk_warning.agent.tool.ToolContext;
import com.example.academic_risk_warning.agent.tool.ToolRegistry;
import com.example.academic_risk_warning.agent.tool.ToolResult;
import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P2-10 / P2R-2 Tutor：带引用的课程答疑契约。
 *
 * <p>检索走统一工具 `retrieveCourseDocs`（与 Diagnostician 同一 ToolSpec/门控/返回结构），
 * 命中经 CitationBinder 绑定到 chunk 原文；每条引用带 chunkId/docId/docName/sectionNo/score。</p>
 */
@Component
@RequiredArgsConstructor
public class TutorAgent {

    private final ToolRegistry registry;
    private final CitationBinder binder;
    private final JdbcTemplate jdbc;

    public Map<String, Object> tutor(long courseId, String question, LoginUser user) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) {
            throw new BizException(400, "问题不能为空");
        }
        List<String> names = jdbc.queryForList("select course_name from course where id=?", String.class, courseId);
        String courseName = names.isEmpty() ? ("课程" + courseId) : names.get(0);
        ToolResult result = registry.execute("retrieveCourseDocs",
                Map.of("courseId", courseId, "query", q, "topK", 4, "mode", "FULLTEXT"), new ToolContext(user, null));
        if (!result.isOk()) {
            throw new BizException(500, "检索工具失败：" + result.getErrorCode());
        }
        Map<String, Object> data = result.getData();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hits = (List<Map<String, Object>>) data.getOrDefault("hits", List.of());
        boolean refused = Boolean.TRUE.equals(data.get("refused"));
        CitationBinder.BindResult bound = binder.bind(hits);
        List<Map<String, Object>> citations = new ArrayList<>();
        for (CitationBinder.Citation c : bound.citations()) {
            citations.add(binder.citationMap(c));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("agent", "tutor");
        out.put("courseId", courseId);
        out.put("courseName", courseName);
        out.put("question", q);
        out.put("refused", refused);
        out.put("gateReason", data.get("gateReason"));
        out.put("citations", citations);
        out.put("citationCount", citations.size());
        out.put("citationsValid", bound.valid());
        out.put("toolDigest", result.getDigest());
        out.put("answer", refused
                ? "这个问题在《" + courseName + "》的课程资料中没有足够可靠的依据，暂不给出结论。建议换一个更具体的问法，或到课堂向老师确认。"
                : answer(courseName, bound.citations()));
        return out;
    }

    private String answer(String courseName, List<CitationBinder.Citation> citations) {
        StringBuilder sb = new StringBuilder("根据《").append(courseName).append("》的课程资料：");
        int used = 0;
        for (CitationBinder.Citation c : citations) {
            if (used >= 3 || c.content() == null || c.content().isBlank()) {
                continue;
            }
            if (used > 0) {
                sb.append("；");
            }
            String text = c.content().trim();
            sb.append(text.length() <= 160 ? text : text.substring(0, 159) + "…");
            used++;
        }
        sb.append("。以上内容仅来自上述资料片段，具体以教师讲解为准。");
        return sb.toString();
    }
}
