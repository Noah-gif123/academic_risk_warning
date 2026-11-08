package com.example.academic_risk_warning.controller.agent;

import com.example.academic_risk_warning.agent.memory.MemoryService;
import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * P2-11 记忆入口：学生本人可查/可删；管理员可查看/重建。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memory;

    @GetMapping("/student/memory")
    @RequireRole("STUDENT")
    public Result<Map<String, Object>> mine() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException(403, "当前账号没有学生档案");
        }
        return Result.ok(memory.get(user.getRefId()));
    }

    @DeleteMapping("/student/memory")
    @RequireRole("STUDENT")
    public Result<Map<String, Object>> deleteMine() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException(403, "当前账号没有学生档案");
        }
        return Result.ok(memory.delete(user.getRefId()));
    }

    @GetMapping("/admin/memory/{studentId}")
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> adminGet(@PathVariable Long studentId) {
        return Result.ok(memory.get(studentId));
    }

    @PostMapping("/admin/memory/{studentId}/rebuild")
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> adminRebuild(@PathVariable Long studentId) {
        return Result.ok(memory.rebuildFromFacts(studentId));
    }
}
