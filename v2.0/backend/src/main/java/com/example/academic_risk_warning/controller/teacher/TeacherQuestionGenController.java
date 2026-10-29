package com.example.academic_risk_warning.controller.teacher;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.AiQuestionGenRequest;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ai.QuestionGenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 出题（M14）：按所选/已上传资料出题 → 五道闸门 → 审题草稿 → 采纳入卷。
 *
 * <p>成题的责任链不变：草稿由模型出、由教师改与采纳，进试卷走原有的试卷保存接口。</p>
 */
@RestController
@RequestMapping("/api/teacher/ai/questions")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherQuestionGenController {

    private final QuestionGenService questionGenService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有教师档案");
        }
        return user.getRefId();
    }

    /** 可出题的资料清单（知识库文档） */
    @GetMapping("/sources")
    public Result<Map<String, Object>> sources(@RequestParam Long teachingClassId) {
        return Result.ok(questionGenService.sources(teachingClassId, me()));
    }

    /** 生成草稿（生成 + 五道闸门，全部落 ai_question_draft） */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody AiQuestionGenRequest request) {
        return Result.ok(questionGenService.generate(request, me()));
    }

    /** 本课程历史草稿（默认全部状态，可按 DRAFT/ADOPTED/REJECTED 过滤） */
    @GetMapping("/drafts")
    public Result<List<Map<String, Object>>> drafts(@RequestParam Long teachingClassId,
                                                    @RequestParam(required = false) String status) {
        return Result.ok(questionGenService.listDrafts(teachingClassId, status, me()));
    }

    /** 教师改题 */
    @PostMapping("/drafts/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.ok(questionGenService.updateDraft(id, body, me()));
    }

    /** 弃用某道草稿 */
    @PostMapping("/drafts/{id}/reject")
    public Result<Map<String, Object>> reject(@PathVariable Long id) {
        return Result.ok(questionGenService.rejectDraft(id, me()));
    }

    /** 采纳（可批量）：只改状态，题目进试卷由前端走试卷保存接口 */
    @PostMapping("/adopt")
    public Result<Map<String, Object>> adopt(@RequestBody Map<String, Object> body) {
        Object raw = body.get("draftIds");
        if (!(raw instanceof List<?> list)) {
            throw new BizException("缺少 draftIds");
        }
        List<Long> ids = list.stream()
                .filter(Objects -> Objects != null)
                .map(v -> Long.parseLong(String.valueOf(v)))
                .toList();
        return Result.ok(questionGenService.adopt(ids, me()));
    }
}
