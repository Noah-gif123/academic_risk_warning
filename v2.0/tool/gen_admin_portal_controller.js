/* gen_admin_portal_controller.js — 1-13 教务端控制器 */
'use strict';
const fs = require('fs');
const path = require('path');

const BE = 'E:/JavaDEVIDEA/workspace/JavaProject/academic_risk_warning/v2.0/backend';
const J = path.join(BE, 'src/main/java/com/example/academic_risk_warning');
const P = 'com.example.academic_risk_warning';

function w(rel, content) {
  const p = path.join(J, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, content.replace(/^\n/, ''), 'utf8');
}

w('controller/admin/AdminPortalController.java', `
package ${P}.controller.admin;

import ${P}.common.Result;
import ${P}.dto.RollbackRequest;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.AdminPortalService;
import ${P}.vo.AdminDashboardVO;
import ${P}.vo.AuditLogPageVO;
import ${P}.vo.ImportBatchVO;
import ${P}.vo.JoinRequestVO;
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
`);

w('dto/RollbackRequest.java', `
package ${P}.dto;

import lombok.Data;

/** 回滚导入批次 */
@Data
public class RollbackRequest {
    private String reason;
}
`);

console.log('教务端模块：控制器生成完成');
