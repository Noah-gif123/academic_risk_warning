package com.example.academic_risk_warning.controller.student;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ai.StudentQaService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 学生端 · 课程答疑（RAG）。
 *
 * <p>入口只在「我的薄弱项 / 预警详情 / 课程详情」里，不做通用聊天窗：
 * 每次提问都带课程上下文，回答带引用，无依据直接拒答。</p>
 */
@RestController
@RequestMapping("/api/student/ai")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentQaController {

    private final StudentQaService qaService;
    private final JdbcTemplate jdbc;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有学生档案");
        }
        return user.getRefId();
    }

    /**
     * body: { courseId? 或 teachingClassId?, question }
     */
    @PostMapping("/qa")
    public Result<Map<String, Object>> ask(@RequestBody Map<String, Object> body) {
        Long courseId = lng(body.get("courseId"));
        Long tc = lng(body.get("teachingClassId"));
        Object q = body.get("question");
        if (q == null || String.valueOf(q).isBlank()) {
            throw new BizException("缺少 question");
        }
        List<Map<String, Object>> history = List.of();
        if (body.get("history") instanceof List<?> list) {
            history = list.stream()
                    .filter(Map.class::isInstance)
                    .map(o -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = (Map<String, Object>) o;
                        return m;
                    })
                    .toList();
        }
        return Result.ok(qaService.ask(me(), courseId, tc, String.valueOf(q), history));
    }

    @PostMapping("/qa/feedback")
    public Result<Void> feedback(@RequestBody Map<String, Object> body) {
        Long runId = lng(body.get("runId"));
        if (runId == null) throw new BizException("缺少 runId");
        boolean useful = Boolean.parseBoolean(String.valueOf(body.get("useful")));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        if (reason != null && reason.length() > 300) reason = reason.substring(0, 300);
        jdbc.update("""
                insert into ai_qa_feedback(run_id, student_id, useful, reason) values(?,?,?,?)
                on duplicate key update useful=values(useful), reason=values(reason)
                """, runId, me(), useful, reason);
        return Result.ok(null);
    }

    private static Long lng(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }
}
