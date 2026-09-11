package com.example.academic_risk_warning.agent.tool;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册表（W3）：Spring 自动收集所有 {@link AgentTool} 实现，
 * 供工具调用环生成"可用工具清单"，并按名字路由调用。
 */
@Component
public class ToolRegistry {

    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public ToolRegistry(List<AgentTool> toolList) {
        if (toolList != null) {
            for (AgentTool tool : toolList) {
                if (tool != null && tool.name() != null) {
                    tools.put(tool.name(), tool);
                }
            }
        }
    }

    public AgentTool get(String name) {
        return name == null ? null : tools.get(name.trim());
    }

    public Collection<AgentTool> all() {
        return tools.values();
    }

    public List<String> names() {
        return List.copyOf(tools.keySet());
    }

    public int size() {
        return tools.size();
    }

    /** 生成给模型看的工具清单文本 */
    public String catalog() {
        StringBuilder sb = new StringBuilder();
        for (AgentTool tool : tools.values()) {
            sb.append("- ").append(tool.name()).append("：").append(tool.description())
              .append("；参数：").append(tool.parameters()).append("\n");
        }
        return sb.toString();
    }
}
