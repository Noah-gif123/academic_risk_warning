package com.example.academic_risk_warning.controller.admin;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.ArchiveReviewRequest;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.AttendanceService;
import com.example.academic_risk_warning.vo.ArchiveVO;
import com.example.academic_risk_warning.vo.CoverageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 教务端 · 考勤归档审批与覆盖率督办 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminAttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/attendance/archives")
    public Result<List<ArchiveVO>> archives(@RequestParam(required = false) Long termId,
                                            @RequestParam(required = false) String status) {
        return Result.ok(attendanceService.listArchivesByTerm(termId, status));
    }

    @PostMapping("/attendance/archives/{id}/review")
    public Result<ArchiveVO> review(@PathVariable Long id,
                                    @RequestParam(defaultValue = "true") boolean approve,
                                    @RequestBody(required = false) ArchiveReviewRequest request) {
        LoginUser user = UserContext.require();
        return Result.ok(attendanceService.review(id, approve, request, user.getAccountId()));
    }

    @PostMapping("/attendance/archives/{id}/remind")
    public Result<ArchiveVO> remind(@PathVariable Long id) {
        LoginUser user = UserContext.require();
        return Result.ok(attendanceService.remind(id, user.getAccountId()));
    }

    @GetMapping("/attendance/coverage")
    public Result<List<CoverageVO>> coverage(@RequestParam(required = false) Long termId) {
        return Result.ok(attendanceService.coverage(termId));
    }
}
