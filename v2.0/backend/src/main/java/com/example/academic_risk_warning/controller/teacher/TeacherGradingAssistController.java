package com.example.academic_risk_warning.controller.teacher;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ai.GradingAssistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 批改辅助（M15）：给主观题出建议分 + 评语，教师确认后生效。
 *
 * <p>注意：这里**没有"直接写分"的接口**。采纳建议只是把"教师最终用了多少分"记下来，
 * 真正的分数写入仍走 {@code POST /api/teacher/submissions/{id}/grade}，判卷人与审计链路不变。</p>
 */
@RestController
@RequestMapping("/api/teacher/ai/grading")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherGradingAssistController {

    private final GradingAssistService gradingAssistService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有教师档案");
        }
        return user.getRefId();
    }

    /** 生成建议（一次调用批完整张卷子的主观题） */
    @PostMapping("/submissions/{submissionId}/suggest")
    public Result<Map<String, Object>> suggest(@PathVariable Long submissionId) {
        return Result.ok(gradingAssistService.generate(submissionId, me()));
    }

    /** 看这份提交已有的建议（重进页面不用重新花钱） */
    @GetMapping("/submissions/{submissionId}/suggestions")
    public Result<List<Map<String, Object>>> list(@PathVariable Long submissionId) {
        return Result.ok(gradingAssistService.list(submissionId, me()));
    }

    /** 采纳（只留痕，不改分） */
    @PostMapping("/suggestions/{id}/apply")
    public Result<Map<String, Object>> apply(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return Result.ok(gradingAssistService.apply(id, body == null ? Map.of() : body, me()));
    }
}
