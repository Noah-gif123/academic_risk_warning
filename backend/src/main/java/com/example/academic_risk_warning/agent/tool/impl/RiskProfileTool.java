package com.example.academic_risk_warning.agent.tool.impl;

import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.tool.AgentTool;
import com.example.academic_risk_warning.service.StudentContextService;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 工具：查学生风险画像（当前预警等级、风险分、五维风险构成） */
@Component
public class RiskProfileTool implements AgentTool {

    private final StudentContextService studentContextService;

    public RiskProfileTool(StudentContextService studentContextService) {
        this.studentContextService = studentContextService;
    }

    @Override
    public String name() {
        return "student_risk_profile";
    }

    @Override
    public String description() {
        return "查询该学生在某门课的风险画像：当前预警等级、风险分、学业/作业/考勤/知识点/历史五维风险分";
    }

    @Override
    public String parameters() {
        return "courseId(可选，缺省用当前课程)";
    }

    @Override
    public Map<String, Object> invoke(AgentContext ctx, Map<String, Object> args) {
        return studentContextService.getStudentRiskProfile(ctx.getStudentId(), ToolArgs.courseId(args, ctx.getCourseId()));
    }
}
