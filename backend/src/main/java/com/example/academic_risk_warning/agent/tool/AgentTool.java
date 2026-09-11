package com.example.academic_risk_warning.agent.tool;

import com.example.academic_risk_warning.agent.core.AgentContext;

import java.util.Map;

/**
 * 智能体可调用的工具（W3 工具调用 / Function Calling）。
 *
 * <p>工具只做"确定性数据获取"，不做判断：大模型负责决定调哪个工具、传什么参数，
 * 真正的取数逻辑在 Java 侧执行，避免模型凭空编造学情数据。
 */
public interface AgentTool {

    /** 工具名（模型按此名调用，需唯一且语义清晰） */
    String name();

    /** 工具用途说明（给模型看） */
    String description();

    /** 参数说明（给模型看的自然语言描述，例如 "courseId(可选)"） */
    default String parameters() {
        return "无参数（学生ID自动注入）";
    }

    /**
     * 执行工具。
     *
     * @param ctx  当前智能体上下文（含 studentId / courseId）
     * @param args 模型给出的参数
     * @return 结构化结果（会被序列化成 JSON 回灌给模型）
     */
    Map<String, Object> invoke(AgentContext ctx, Map<String, Object> args);
}
