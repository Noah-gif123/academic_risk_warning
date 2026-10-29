package com.example.academic_risk_warning.controller.teacher;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.service.ai.KbService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 课程知识库（M11）：入库 / 状态 / 检索调试。
 *
 * <p>检索接口先做成"调试口"，方便在命令行就能看到命中片段（文档名 + 第几段 + 相关度），
 * 再接生成与页面。</p>
 */
@RestController
@RequestMapping("/api/teacher/ai/kb")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherKbController {

    private final KbService kbService;

    /** 入库（幂等：内容未变不会重复写） */
    @PostMapping("/sync")
    public Result<Map<String, Object>> sync(@RequestParam Long courseId) {
        return Result.ok(kbService.syncCourse(courseId));
    }

    /** 当前课程知识库状态：文档数、分块数、每份文档的分块情况 */
    @GetMapping("/status")
    public Result<Map<String, Object>> status(@RequestParam Long courseId) {
        return Result.ok(kbService.status(courseId));
    }

    /** 检索调试：返回命中的片段与相关度 */
    @PostMapping("/retrieve")
    public Result<List<Map<String, Object>>> retrieve(@RequestBody Map<String, Object> body) {
        Object cid = body.get("courseId");
        Object q = body.get("query");
        int topK = body.get("topK") == null ? 5 : Integer.parseInt(String.valueOf(body.get("topK")));
        if (cid == null || q == null) {
            return Result.fail(400, "缺少 courseId 或 query");
        }
        return Result.ok(kbService.retrieve(Long.parseLong(String.valueOf(cid)), String.valueOf(q), topK));
    }
}
