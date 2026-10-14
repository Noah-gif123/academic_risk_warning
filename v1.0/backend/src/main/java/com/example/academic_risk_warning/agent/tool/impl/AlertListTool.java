package com.example.academic_risk_warning.agent.tool.impl;

import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.tool.AgentTool;
import com.example.academic_risk_warning.service.AlertQueryService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 工具：查学生历史预警记录 */
@Component
public class AlertListTool implements AgentTool {

    private final AlertQueryService alertQueryService;

    public AlertListTool(AlertQueryService alertQueryService) {
        this.alertQueryService = alertQueryService;
    }

    @Override
    public String name() {
        return "student_alerts";
    }

    @Override
    public String description() {
        return "查询该学生的历史预警记录（预警等级、风险分、预警原因、处理状态），可用于统计「被预警过几次」";
    }

    @Override
    public String parameters() {
        return "status(可选：ACTIVE/HANDLED/CLOSED，缺省全部)，limit(可选，默认5)";
    }

    @Override
    public Map<String, Object> invoke(AgentContext ctx, Map<String, Object> args) {
        String status = ToolArgs.strOf(args, "status");
        int limit = Math.max(1, Math.min(ToolArgs.intOf(args, "limit", 5), 20));
        List<?> alerts = alertQueryService.getAlertsByStudentId(ctx.getStudentId(),
                status == null || status.isBlank() ? null : status);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", ctx.getStudentId());
        result.put("total", alerts == null ? 0 : alerts.size());
        result.put("statusFilter", status == null || status.isBlank() ? "ALL" : status);
        result.put("alerts", alerts == null ? List.of() : alerts.stream().limit(limit).toList());
        return result;
    }
}
