package com.example.academic_risk_warning.controller.student;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ai.QaSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/ai/sessions")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentQaSessionController {

    private final QaSessionService service;

    private Long me() {
        LoginUser u = UserContext.require();
        if (u.getRefId() == null) throw new BizException("当前账号没有学生档案");
        return u.getRefId();
    }

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(required = false) Long courseId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return Result.ok(service.list(me(), courseId, page, size));
    }

    @PatchMapping("/{id}")
    public Result<Map<String, Object>> rename(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int n = service.rename(me(), id, body.get("title") == null ? null : String.valueOf(body.get("title")));
        return Result.ok(Map.of("updated", n));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Long courseId = lng(body.get("courseId"));
        Long tc = lng(body.get("teachingClassId"));
        if (courseId == null) throw new BizException("缺少 courseId");
        return Result.ok(service.create(me(), courseId, tc,
                body.get("title") == null ? null : String.valueOf(body.get("title"))));
    }

    @GetMapping("/{id}/messages")
    public Result<List<Map<String, Object>>> messages(@PathVariable Long id) {
        return Result.ok(service.messages(me(), id));
    }

    @DeleteMapping("/{id}")
    public Result<Map<String, Object>> delete(@PathVariable Long id) {
        return Result.ok(Map.of("deleted", service.delete(me(), id)));
    }

    private static Long lng(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v).trim()); } catch (Exception e) { return null; }
    }
}

