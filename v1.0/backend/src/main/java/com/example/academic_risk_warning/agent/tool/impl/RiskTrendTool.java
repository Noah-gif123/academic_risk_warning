package com.example.academic_risk_warning.agent.tool.impl;

import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.tool.AgentTool;
import com.example.academic_risk_warning.service.AlertQueryService;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 工具：查学生风险变化趋势（按日期序列） */
@Component
public class RiskTrendTool implements AgentTool {

    private final AlertQueryService alertQueryService;

    public RiskTrendTool(AlertQueryService alertQueryService) {
        this.alertQueryService = alertQueryService;
    }

    @Override
    public String name() {
        return "student_risk_trend";
    }

    @Override
    public String description() {
        return "查询该学生某门课的风险变化趋势（按时间的风险分序列，可判断是在恶化还是好转）";
    }

    @Override
    public String parameters() {
        return "courseId(可选，缺省用当前课程)";
    }

    @Override
    public Map<String, Object> invoke(AgentContext ctx, Map<String, Object> args) {
        return alertQueryService.getStudentRiskTrend(ctx.getStudentId(), ToolArgs.courseId(args, ctx.getCourseId()));
    }
}
