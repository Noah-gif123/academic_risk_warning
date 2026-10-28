package com.example.academic_risk_warning.controller.student;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ai.AlertExplainService;
import com.example.academic_risk_warning.vo.ai.AlertExplainVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生端 · 我的预警解释卡（M9）。
 *
 * <p>只允许查看自己名下的预警；解释卡里的数字与判定口径一致，可逐项对照。</p>
 */
@RestController
@RequestMapping("/api/student/alerts")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentAlertExplainController {

    private final AlertExplainService alertExplainService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有学生档案");
        }
        return user.getRefId();
    }

    private void assertMine(Long alertId) {
        Long owner = alertExplainService.alertStudentId(alertId);
        if (!me().equals(owner)) {
            throw new BizException("只能查看自己的预警解释");
        }
    }

    @PostMapping("/{alertId}/explain")
    public Result<AlertExplainVO> generate(@PathVariable Long alertId) {
        assertMine(alertId);
        return Result.ok(alertExplainService.generate(alertId, me()));
    }

    @GetMapping("/{alertId}/explain")
    public Result<AlertExplainVO> latest(@PathVariable Long alertId) {
        assertMine(alertId);
        return Result.ok(alertExplainService.latest(alertId));
    }
}
