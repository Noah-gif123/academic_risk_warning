package com.example.academic_risk_warning.controller.admin;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.RiskRuleRequest;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.RuleConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 教务端 · 判定规则配置（第二阶段第 5 步）。
 *
 * <p>规则按版本管理：改规则是「新增版本并生效」，历史版本可回切，所有变更写审计日志。</p>
 */
@RestController
@RequestMapping("/api/admin/rules")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminRuleController {

    private final RuleConfigService ruleConfigService;

    /** 当前生效版本 + 历史版本列表 */
    @GetMapping
    public Result<Map<String, Object>> list() {
        return Result.ok(ruleConfigService.list());
    }

    /** 新增版本并立即生效（未传字段沿用当前版本） */
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody RiskRuleRequest request) {
        return Result.ok(ruleConfigService.create(UserContext.require(), request));
    }

    /** 切换生效版本（含回切历史版本） */
    @PostMapping("/{id}/activate")
    public Result<Map<String, Object>> activate(@PathVariable Long id) {
        return Result.ok(ruleConfigService.activate(UserContext.require(), id));
    }
}
