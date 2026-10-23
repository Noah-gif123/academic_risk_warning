package com.example.academic_risk_warning.controller.student;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.AlertEventService;
import com.example.academic_risk_warning.service.StudentWarningService;
import com.example.academic_risk_warning.vo.AlertRowVO;
import com.example.academic_risk_warning.vo.StudentWarningVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 学生端 · 我的学情预警（第二阶段第 4 步）。
 *
 * <p>只读结论 + 申诉入口：一门课一条结论，只有结论与建议，不含教师内部处置记录；
 * 申诉写到 alert_record.student_response 并通知任课教师与辅导员。</p>
 */
@RestController
@RequestMapping("/api/student/warnings")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentWarningController {

    private final StudentWarningService studentWarningService;
    private final AlertEventService alertEventService;

    /** 概览：每门课一条结论 + 建议 + 判定口径（termId 不传为当前学期） */
    @GetMapping
    public Result<StudentWarningVO> overview(@RequestParam(required = false) Long termId) {
        return Result.ok(studentWarningService.overview(UserContext.require(), termId));
    }

    /** 单课详情：结论 + 学生可见时间线 + 建议 + 口径 */
    @GetMapping("/{enrollmentId}")
    public Result<StudentWarningVO.Detail> detail(@PathVariable Long enrollmentId) {
        return Result.ok(studentWarningService.detail(UserContext.require(), enrollmentId));
    }

    /** 提交申诉（每门课只能申诉一次） */
    @PostMapping("/{alertId}/appeal")
    public Result<AlertRowVO> appeal(@PathVariable Long alertId, @RequestBody AppealRequest request) {
        return Result.ok(alertEventService.studentAppeal(UserContext.require(), alertId,
                request == null ? null : request.getReason()));
    }

    /** 申诉请求体 */
    @lombok.Data
    public static class AppealRequest {
        private String reason;
    }
}
