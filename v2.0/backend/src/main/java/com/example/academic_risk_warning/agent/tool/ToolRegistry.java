package com.example.academic_risk_warning.agent.tool;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * P2-2 工具注册表：9 工具可调用、参数强校验、超时、结果摘要。
 *
 * <p>参数校验规则：未知参数拒绝、必填检查、类型检查（integer/string/boolean）、
 * 枚举、min/max、字符串长度；校验失败返回 INVALID_ARGS，不进入 handler。</p>
 */
@Component
public class ToolRegistry {

    private final ToolSpecLoader loader;
    private final Map<String, ToolHandler> handlers;
    private final ObjectMapper json = new ObjectMapper();
    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "tool-executor");
        t.setDaemon(true);
        return t;
    });

    public ToolRegistry(ToolSpecLoader loader, BuiltinTools builtin) {
        this.loader = loader;
        this.handlers = builtin.handlers();
        if (!handlers.keySet().equals(new java.util.LinkedHashSet<>(loader.list().stream().map(ToolSpec::getName).toList()))) {
            throw new IllegalStateException("Registry 实现与 tools.yaml 不一致：" + handlers.keySet());
        }
    }

    public ToolSpec spec(String name) {
        return loader.get(name);
    }

    public List<ToolSpec> specs() {
        return loader.list();
    }

    /** 校验参数；返回 null 表示通过，否则返回错误信息（供 P2-3 四道检查复用）。 */
    public String validateArgs(String name, Map<String, Object> args) {
        ToolSpec spec = loader.get(name);
        if (spec == null) {
            return "未注册的工具：" + name;
        }
        Map<String, Object> a = args == null ? Map.of() : args;
        for (String key : a.keySet()) {
            if (!spec.getParams().containsKey(key)) {
                return "未知参数：" + key;
            }
        }
        for (Map.Entry<String, ToolSpec.Param> e : spec.getParams().entrySet()) {
            String key = e.getKey();
            ToolSpec.Param p = e.getValue();
            Object v = a.get(key);
            if (v == null) {
                if (p.isRequired()) {
                    return "缺少必填参数：" + key;
                }
                continue;
            }
            switch (p.getType()) {
                case "integer": {
                    if (!(v instanceof Number)) {
                        return key + " 应为整数";
                    }
                    int n = ((Number) v).intValue();
                    if (p.getMin() != null && n < p.getMin()) return key + " 小于最小值 " + p.getMin();
                    if (p.getMax() != null && n > p.getMax()) return key + " 大于最大值 " + p.getMax();
                    break;
                }
                case "string": {
                    if (!(v instanceof String)) {
                        return key + " 应为字符串";
                    }
                    int len = ((String) v).length();
                    if (p.getMinLength() != null && len < p.getMinLength()) return key + " 长度不足";
                    if (p.getMaxLength() != null && len > p.getMaxLength()) return key + " 长度超限";
                    if (!p.getEnumValues().isEmpty() && !p.getEnumValues().contains(v)) {
                        return key + " 不在枚举范围：" + p.getEnumValues();
                    }
                    break;
                }
                case "boolean": {
                    if (!(v instanceof Boolean)) return key + " 应为布尔值";
                    break;
                }
                default:
                    return key + " 类型未支持：" + p.getType();
            }
        }
        return null;
    }

    /** 执行工具（含超时与结果摘要）。 */
    public ToolResult execute(String name, Map<String, Object> args, ToolContext ctx) {
        ToolSpec spec = loader.get(name);
        if (spec == null) {
            return ToolResult.fail("UNKNOWN_TOOL", "未注册的工具：" + name);
        }
        String invalid = validateArgs(name, args);
        if (invalid != null) {
            return ToolResult.fail("INVALID_ARGS", invalid);
        }
        ToolHandler handler = handlers.get(name);
        if (handler == null) {
            return ToolResult.fail("UNKNOWN_TOOL", "工具未实现：" + name);
        }
        long t0 = System.currentTimeMillis();
        Future<ToolResult> future = pool.submit(() -> handler.execute(ctx, args == null ? Map.of() : args));
        ToolResult result;
        try {
            result = future.get(spec.getTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            result = ToolResult.fail("TOOL_TIMEOUT", "工具超时（" + spec.getTimeoutMs() + "ms）");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            result = ToolResult.fail("TOOL_ERROR", cause.getClass().getSimpleName() + ": " + cause.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = ToolResult.fail("TOOL_INTERRUPTED", "工具执行被中断");
        }
        result.setElapsedMs(System.currentTimeMillis() - t0);
        try {
            String text = json.writeValueAsString(result.getData()) + json.writeValueAsString(result.getEvidence());
            result.setBytes(text.getBytes(StandardCharsets.UTF_8).length);
            result.setDigest(sha256(text).substring(0, 16));
        } catch (Exception ignored) {
            result.setDigest(null);
        }
        return result;
    }

    /** 给 P2-3/OpenAPI/测试用的只读摘要。 */
    public List<Map<String, Object>> specSummaries() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ToolSpec s : loader.list()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", s.getName());
            m.put("description", s.getDescription());
            m.put("read_only", s.isReadOnly());
            m.put("timeout_ms", s.getTimeoutMs());
            m.put("evidence_fields", s.getEvidenceFields());
            m.put("params", s.getParams());
            out.add(m);
        }
        return out;
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
