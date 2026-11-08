package com.example.academic_risk_warning.agent.exec;

import com.example.academic_risk_warning.agent.tool.ToolContext;
import com.example.academic_risk_warning.agent.tool.ToolRegistry;
import com.example.academic_risk_warning.agent.tool.ToolResult;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P2-3 受限 JSON 工具执行器：四道检查（可解析 / 白名单 / 强校验 / 步数预算）。
 *
 * <p>模型（或测试）只能提交 {@code {"tool":"...","args":{...}}} 形式的 JSON；
 * 任何不合法调用在进入 handler 之前被拦截，并记录每道检查的结果。</p>
 */
@Component
@RequiredArgsConstructor
public class ToolCallExecutor {

    private final ToolRegistry registry;
    private final ObjectMapper json = new ObjectMapper();

    public record Check(String name, boolean ok, String detail) {
    }

    public record Outcome(boolean ok, String tool, Map<String, Object> args, List<Check> checks,
                          ToolResult result, int usedStepsAfter) {
    }

    public Outcome execute(String rawJson, ToolContext ctx, int usedSteps, int maxSteps) {
        List<Check> checks = new ArrayList<>();
        Map<String, Object> parsed;
        try {
            if (rawJson == null || rawJson.isBlank()) {
                throw new IllegalArgumentException("空 JSON");
            }
            parsed = json.readValue(rawJson, new TypeReference<Map<String, Object>>() {
            });
            checks.add(new Check("parse", true, "JSON 可解析"));
        } catch (Exception e) {
            checks.add(new Check("parse", false, "JSON 解析失败：" + e.getMessage()));
            return new Outcome(false, null, Map.of(), checks,
                    ToolResult.fail("PARSE_ERROR", "工具调用必须是合法 JSON"), usedSteps);
        }
        Object toolObj = parsed.get("tool");
        if (!(toolObj instanceof String tool) || tool.isBlank()) {
            checks.add(new Check("whitelist", false, "缺少 tool 字段"));
            return new Outcome(false, null, Map.of(), checks,
                    ToolResult.fail("INVALID_CALL", "缺少 tool 字段"), usedSteps);
        }
        if (registry.spec(tool) == null) {
            checks.add(new Check("whitelist", false, "工具不在白名单：" + tool));
            return new Outcome(false, tool, Map.of(), checks,
                    ToolResult.fail("UNKNOWN_TOOL", "工具不在白名单：" + tool), usedSteps);
        }
        checks.add(new Check("whitelist", true, "工具在白名单：" + tool));

        Object argsObj = parsed.get("args");
        if (argsObj != null && !(argsObj instanceof Map)) {
            checks.add(new Check("schema", false, "args 必须是对象"));
            return new Outcome(false, tool, Map.of(), checks,
                    ToolResult.fail("INVALID_ARGS", "args 必须是对象"), usedSteps);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> args = argsObj == null ? new LinkedHashMap<>() : (Map<String, Object>) argsObj;
        String invalid = registry.validateArgs(tool, args);
        if (invalid != null) {
            checks.add(new Check("schema", false, invalid));
            return new Outcome(false, tool, args, checks, ToolResult.fail("INVALID_ARGS", invalid), usedSteps);
        }
        checks.add(new Check("schema", true, "参数通过 schema 校验"));

        if (usedSteps >= maxSteps) {
            checks.add(new Check("budget", false, "步数预算已用尽（" + usedSteps + "/" + maxSteps + "）"));
            return new Outcome(false, tool, args, checks,
                    ToolResult.fail("STEP_BUDGET_EXCEEDED", "步数预算已用尽"), usedSteps);
        }
        checks.add(new Check("budget", true, "步数 " + (usedSteps + 1) + "/" + maxSteps));

        ToolResult result = registry.execute(tool, args, ctx);
        return new Outcome(result.isOk(), tool, args, checks, result, usedSteps + 1);
    }
}
