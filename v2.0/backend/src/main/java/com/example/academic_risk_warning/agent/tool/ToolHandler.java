package com.example.academic_risk_warning.agent.tool;

import java.util.Map;

/** 工具处理器：参数已由 Registry 校验。 */
@FunctionalInterface
public interface ToolHandler {
    ToolResult execute(ToolContext ctx, Map<String, Object> args) throws Exception;
}
