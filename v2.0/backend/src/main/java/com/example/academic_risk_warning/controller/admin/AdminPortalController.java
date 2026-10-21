package com.example.academic_risk_warning.controller.admin;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.RollbackRequest;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.AdminPortalService;
import com.example.academic_risk_warning.vo.AdminDashboardVO;
import com.example.academic_risk_warning.vo.AuditLogPageVO;
import com.example.academic_risk_warning.vo.ImportBatchVO;
import com.example.academic_risk_warning.vo.JoinRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 教务端 · 工作台 / 导入批次 / 入课知会 / 审计日志 / 覆盖率督办 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminPortalController {

    private final AdminPortalService adminPortalService;

    private Long me() {
        LoginUser user = UserContext.require();
        return user.getAccountId();
    }

    @GetMapping("/dashboard")
    public Result<AdminDashboardVO> dashboard(@RequestParam(required = false) Long termId) {
        return Result.ok(adminPortalService.dashboard(termId));
    }

    @GetMapping("/import-batches")
    public Result<List<ImportBatchVO>> batches(@RequestParam(required = false) Long termId,
                                               @RequestParam(required = false) String status) {
        return Result.ok(adminPortalService.importBatches(termId, status));
    }

    @PostMapping("/import-batches/{id}/rollback")
    public Result<ImportBatchVO> rollback(@PathVariable Long id,
                                          @RequestBody(required = false) RollbackRequest request) {
        return Result.ok(adminPortalService.rollback(id,
                request == null ? null : request.getReason(), me()));
    }

    @GetMapping("/join-notices")
    public Result<List<JoinRequestVO>> notices(@RequestParam(required = false) Long termId) {
        return Result.ok(adminPortalService.joinNotices(termId));
    }

    @GetMapping("/audit")
    public Result<AuditLogPageVO> audit(@RequestParam(required = false) String bizType,
                                        @RequestParam(required = false) String action,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String from,
                                        @RequestParam(required = false) String to,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return Result.ok(adminPortalService.auditLogs(bizType, action, keyword, from, to, page, size));
    }

    @PostMapping("/coverage/{teachingClassId}/urge")
    public Result<Void> urge(@PathVariable Long teachingClassId) {
        adminPortalService.urgeCoverage(teachingClassId, me());
        return Result.ok();
    }
}
