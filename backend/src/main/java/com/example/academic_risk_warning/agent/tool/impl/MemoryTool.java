package com.example.academic_risk_warning.agent.tool.impl;

import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.tool.AgentTool;
import com.example.academic_risk_warning.entity.StudentMemory;
import com.example.academic_risk_warning.service.StudentMemoryService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** 工具：查长期记忆（上次画像/策略/干预与最近计划的结论） */
@Component
public class MemoryTool implements AgentTool {

    private final StudentMemoryService studentMemoryService;

    public MemoryTool(StudentMemoryService studentMemoryService) {
        this.studentMemoryService = studentMemoryService;
    }

    @Override
    public String name() {
        return "student_memory";
    }

    @Override
    public String description() {
        return "查询该学生的长期记忆：上一次的画像结论、策略调整、干预措施与学习计划，用于回答「之前给过什么建议」";
    }

    @Override
    public String parameters() {
        return "courseId(可选，缺省用当前课程)";
    }

    @Override
    public Map<String, Object> invoke(AgentContext ctx, Map<String, Object> args) {
        Long courseId = ToolArgs.courseId(args, ctx.getCourseId());
        StudentMemory memory = studentMemoryService.getLatest(ctx.getStudentId(), courseId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", ctx.getStudentId());
        result.put("hasMemory", memory != null);
        if (memory != null) {
            result.put("version", memory.getVersion());
            result.put("createTime", String.valueOf(memory.getCreateTime()));
            result.put("summary", memory.getSummary());
        } else {
            result.put("summary", "暂无历史记忆（该学生还没有跑过完整评估）");
        }
        return result;
    }
}
