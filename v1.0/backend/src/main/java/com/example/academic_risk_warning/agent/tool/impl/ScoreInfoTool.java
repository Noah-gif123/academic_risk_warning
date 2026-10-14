package com.example.academic_risk_warning.agent.tool.impl;

import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.tool.AgentTool;
import com.example.academic_risk_warning.service.StudentContextService;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 工具：查学生成绩与平时表现（均分、作业、考勤、挂科情况） */
@Component
public class ScoreInfoTool implements AgentTool {

    private final StudentContextService studentContextService;

    public ScoreInfoTool(StudentContextService studentContextService) {
        this.studentContextService = studentContextService;
    }

    @Override
    public String name() {
        return "student_score_info";
    }

    @Override
    public String description() {
        return "查询该学生某门课的成绩信息：综合得分、作业完成情况、考勤、挂科次数、历史成绩等客观数据";
    }

    @Override
    public String parameters() {
        return "courseId(可选，缺省用当前课程)";
    }

    @Override
    public Map<String, Object> invoke(AgentContext ctx, Map<String, Object> args) {
        return studentContextService.getStudentScoreInfo(ctx.getStudentId(), ToolArgs.courseId(args, ctx.getCourseId()));
    }
}
