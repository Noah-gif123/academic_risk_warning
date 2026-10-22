package com.example.academic_risk_warning.controller.teacher;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.RiskEngineService;
import com.example.academic_risk_warning.vo.RiskRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教师端 · 学情预警（第二阶段第 1 步先提供只读名单与总览，
 * 页面在第 3 步接入；此处接口即为页面数据源）。
 */
@RestController
@RequestMapping("/api/teacher/risks")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherRiskController {

    private final RiskEngineService riskEngineService;

    /** 风险名单：按等级与关键词过滤，按风险分倒序 */
    @GetMapping
    public Result<List<RiskRowVO>> list(@RequestParam Long teachingClassId,
                                        @RequestParam(required = false) String level,
                                        @RequestParam(required = false) String keyword) {
        return Result.ok(riskEngineService.listRisks(UserContext.require(), teachingClassId, level, keyword));
    }

    /** 总览：红/橙/黄/绿计数、硬门槛人数、最近判定时间与引擎版本 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam Long teachingClassId) {
        return Result.ok(riskEngineService.stats(UserContext.require(), teachingClassId));
    }
}
