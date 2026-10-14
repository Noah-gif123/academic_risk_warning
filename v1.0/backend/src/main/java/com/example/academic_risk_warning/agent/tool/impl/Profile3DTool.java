package com.example.academic_risk_warning.agent.tool.impl;

import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.tool.AgentTool;
import com.example.academic_risk_warning.service.StudentProfileService;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 工具：查三维学情画像（知识 / 习惯 / 目标） */
@Component
public class Profile3DTool implements AgentTool {

    private final StudentProfileService studentProfileService;

    public Profile3DTool(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @Override
    public String name() {
        return "student_3d_profile";
    }

    @Override
    public String description() {
        return "查询该学生的三维学情画像（知识水平、学习习惯、目标规划三个维度及对应建议）";
    }

    @Override
    public String parameters() {
        return "courseId(可选，缺省用当前课程)";
    }

    @Override
    public Map<String, Object> invoke(AgentContext ctx, Map<String, Object> args) {
        return studentProfileService.getProfile(ctx.getStudentId(), ToolArgs.courseId(args, ctx.getCourseId()));
    }
}
