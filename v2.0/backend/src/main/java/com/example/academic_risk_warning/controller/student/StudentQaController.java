package com.example.academic_risk_warning.controller.student;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ai.StudentQaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return Result.ok(qaService.ask(me(), courseId, tc, String.valueOf(q)));
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
