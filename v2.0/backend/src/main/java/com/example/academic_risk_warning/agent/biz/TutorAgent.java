package com.example.academic_risk_warning.agent.biz;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.service.ai.StudentQaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P2-10 Tutor：课程答疑的唯一入口适配器。
 *
 * <p>不再维护第二套检索/回答逻辑：直接调用 {@link StudentQaService}，
 * 由它统一完成课程权限、检索、门控、拒答与引用校验；本类只补 agent 字段与 chunkId 映射。</p>
 */
@Component
@RequiredArgsConstructor
public class TutorAgent {

    private final StudentQaService qaService;

    public Map<String, Object> tutor(long courseId, String question, LoginUser user) {
        if (user == null || user.getRefId() == null) {
            throw new BizException(403, "当前账号没有学生档案");
        }
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) {
            throw new BizException(400, "问题不能为空");
        }
        Map<String, Object> r = qaService.ask(user.getRefId(), courseId, null, q, List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hits = (List<Map<String, Object>>) r.getOrDefault("hits", List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawCitations = (List<Map<String, Object>>) r.getOrDefault("citations", List.of());
        List<Map<String, Object>> citations = new ArrayList<>();
        boolean valid = true;
        for (Map<String, Object> c : rawCitations) {
            Map<String, Object> item = new LinkedHashMap<>(c);
            Map<String, Object> hit = findHit(hits, c);
            if (hit == null) {
                valid = false;
            } else {
                item.put("chunkId", hit.get("chunkId"));
                item.put("docId", hit.get("docId"));
                item.put("score", hit.get("score"));
                item.put("rrfRank", hit.get("rrfRank"));
                item.put("content", hit.get("content"));
            }
            citations.add(item);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("agent", "tutor");
        out.put("courseId", courseId);
        out.put("courseName", r.get("courseName"));
        out.put("question", q);
        out.put("answer", r.get("answer"));
        out.put("refused", r.get("refused"));
        out.put("gateReason", r.get("gateReason"));
        out.put("citations", citations);
        out.put("citationCount", citations.size());
        out.put("citationsValid", valid);
        out.put("runId", r.get("runId"));
        out.put("toolDigest", "STUDENT_QA_SERVICE");
        return out;
    }

    private Map<String, Object> findHit(List<Map<String, Object>> hits, Map<String, Object> citation) {
        for (Map<String, Object> h : hits) {
            if (String.valueOf(h.get("docName")).equals(String.valueOf(citation.get("docName")))
                    && String.valueOf(h.get("sectionNo")).equals(String.valueOf(citation.get("sectionNo")))) {
                return h;
            }
        }
        return null;
    }
}
