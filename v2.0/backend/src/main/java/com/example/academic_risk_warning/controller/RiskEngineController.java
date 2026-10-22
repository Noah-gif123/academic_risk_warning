package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.RiskEngineService;
import com.example.academic_risk_warning.vo.RiskEvaluateResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 风险判定引擎（第二阶段第 1 步）。
 *
 * <p>范围优先级：enrollmentId &gt; teachingClassId &gt; offeringId &gt; termId（不传则当前学期）。
 * 教师只能按自己的教学班触发；教务可按学期/开课/选课任意范围触发。</p>
 */
@RestController
@RequestMapping("/api/engine")
@RequiredArgsConstructor
@RequireRole({ "TEACHER", "ADMIN" })
public class RiskEngineController {

    private final RiskEngineService riskEngineService;

    /** 触发一次判定：写 risk_state（一人一课一行，覆盖刷新），返回判定统计 */
    @PostMapping("/evaluate")
    public Result<RiskEvaluateResultVO> evaluate(@RequestParam(required = false) Long termId,
                                                 @RequestParam(required = false) Long offeringId,
                                                 @RequestParam(required = false) Long teachingClassId,
                                                 @RequestParam(required = false) Long enrollmentId,
                                                 @RequestParam(required = false) String trigger) {
        LoginUser user = UserContext.require();
        return Result.ok(riskEngineService.evaluate(user, termId, offeringId, teachingClassId, enrollmentId, trigger));
    }
}
