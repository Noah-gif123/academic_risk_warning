package com.example.academic_risk_warning.agent.tool.impl;

import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.tool.AgentTool;
import com.example.academic_risk_warning.service.StudentContextService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 工具：查学生选修了哪些课程（含及格线） */
@Component
public class StudentCoursesTool implements AgentTool {

    private final StudentContextService studentContextService;

    public StudentCoursesTool(StudentContextService studentContextService) {
        this.studentContextService = studentContextService;
    }

    @Override
    public String name() {
        return "student_courses";
    }

    @Override
    public String description() {
        return "查询该学生选修的全部课程（课程ID、课程名、及格分）";
    }

    @Override
    public Map<String, Object> invoke(AgentContext ctx, Map<String, Object> args) {
        List<Map<String, Object>> courses = studentContextService.getStudentCourses(ctx.getStudentId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", ctx.getStudentId());
        result.put("courseCount", courses.size());
        result.put("courses", courses);
        return result;
    }
}
