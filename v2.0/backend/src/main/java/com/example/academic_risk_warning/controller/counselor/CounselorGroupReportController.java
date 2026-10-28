package com.example.academic_risk_warning.controller.counselor;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ai.GroupReportService;
import com.example.academic_risk_warning.vo.ai.GroupReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 辅导员端 · 行政班学情报告（M8）。
 *
 * <p>与教师端教学班报告互补：教学班是“一门课 × 一个班”，行政班是“一个班 × 多门课”，
 * 重点看集中风险课程与多门预警学生。</p>
 */
@RestController
@RequestMapping("/api/counselor/reports")
@RequiredArgsConstructor
@RequireRole("COUNSELOR")
public class CounselorGroupReportController {

    private final GroupReportService groupReportService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有辅导员档案");
        }
        return user.getRefId();
    }

    @PostMapping("/admin-class/{adminClassId}")
    public Result<GroupReportVO> generate(@PathVariable Long adminClassId) {
        Long counselorId = me();
        groupReportService.assertCounselorOwns(adminClassId, counselorId);
        return Result.ok(groupReportService.generateAdminClassReport(adminClassId, counselorId));
    }

    @GetMapping("/admin-class/{adminClassId}/latest")
    public Result<GroupReportVO> latest(@PathVariable Long adminClassId) {
        groupReportService.assertCounselorOwns(adminClassId, me());
        return Result.ok(groupReportService.latest("ADMIN_CLASS", adminClassId));
    }

    @GetMapping("/admin-class/{adminClassId}/history")
    public Result<List<GroupReportVO>> history(@PathVariable Long adminClassId,
                                               @RequestParam(defaultValue = "10") int limit) {
        groupReportService.assertCounselorOwns(adminClassId, me());
        return Result.ok(groupReportService.list("ADMIN_CLASS", adminClassId, Math.min(limit, 50)));
    }
}
