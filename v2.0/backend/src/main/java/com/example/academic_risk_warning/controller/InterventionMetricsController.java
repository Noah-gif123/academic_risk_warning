package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ai.InterventionMetricsService;
import com.example.academic_risk_warning.vo.ai.InterventionMetricsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 干预闭环度量（M10）：教务端看全校，辅导员端只看自己带的行政班。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InterventionMetricsController {

    private final InterventionMetricsService metricsService;

    /** 教务/管理员：全校口径 */
    @GetMapping("/admin/metrics/intervention")
    @RequireRole("ADMIN")
    public Result<InterventionMetricsVO> adminMetrics(@RequestParam(required = false) Long termId) {
        return Result.ok(metricsService.metrics(termId, null));
    }

    /** 辅导员：本人名下行政班口径 */
    @GetMapping("/counselor/metrics/intervention")
    @RequireRole("COUNSELOR")
    public Result<InterventionMetricsVO> counselorMetrics(@RequestParam(required = false) Long termId) {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有辅导员档案");
        }
        return Result.ok(metricsService.metrics(termId, user.getRefId()));
    }
}
