package com.example.academic_risk_warning.agent.tool.impl;

import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.tool.AgentTool;
import com.example.academic_risk_warning.service.StudentContextService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 工具：查学生薄弱知识点 */
@Component
public class WeakPointTool implements AgentTool {

    private final StudentContextService studentContextService;

    public WeakPointTool(StudentContextService studentContextService) {
        this.studentContextService = studentContextService;
    }

    @Override
    public String name() {
        return "student_weak_points";
    }

    @Override
    public String description() {
        return "查询该学生在某门课的薄弱知识点清单（知识点名称、掌握度、错误次数）";
    }

    @Override
    public String parameters() {
        return "courseId(可选，缺省用当前课程)";
    }

    @Override
    public Map<String, Object> invoke(AgentContext ctx, Map<String, Object> args) {
        List<Map<String, Object>> weakPoints =
                studentContextService.getStudentWeakPoints(ctx.getStudentId(), ToolArgs.courseId(args, ctx.getCourseId()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", ctx.getStudentId());
        result.put("weakPointCount", weakPoints == null ? 0 : weakPoints.size());
        result.put("weakPoints", weakPoints == null ? List.of() : weakPoints);
        return result;
    }
}
