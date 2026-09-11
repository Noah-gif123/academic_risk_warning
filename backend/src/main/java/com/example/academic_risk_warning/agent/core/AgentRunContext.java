package com.example.academic_risk_warning.agent.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 智能体运行上下文（请求线程内有效）。
 *
 * <p>编排器在一次流水线开始时 {@link #begin()}，{@link BaseAgent} 每执行一步就往缓冲里记一条，
 * 流水线结束时由编排器 {@link #collectAndClear()} 统一落库到 {@code agent_run / agent_run_step}。
 * 这样 BaseAgent 不需要引入额外依赖，也不用改动十几处 {@code agent.execute(ctx)} 调用点。
 *
 * <p>W2 起还负责携带本步的<b>校验结果</b>与<b>反思信息</b>（初稿问题、重写轮次）。
 */
public final class AgentRunContext {

    /**
     * 单步执行记录
     *
     * @param validationStatus PASS / WARN / FAIL / SKIP
     * @param validationDetail 校验说明（失败原因等）
     * @param reflection       反思信息（自评/重写原因），没有则为 null
     * @param attempts         LLM 调用轮次（>1 表示触发了反思重写）
     * @param outputFields     产出顶层字段清单（用于 golden set 精确核对，摘要被截断也不影响）
     */
    public record StepRecord(String agentName, String status, long durationMs, String outputDigest,
                             String error, String validationStatus, String validationDetail,
                             String reflection, int attempts, String outputFields) {}

    private static final ThreadLocal<List<StepRecord>> BUFFER = new ThreadLocal<>();

    /** 当前步骤的临时状态（记录前由 BaseAgent/子类填充） */
    private static final ThreadLocal<PendingStep> PENDING = new ThreadLocal<>();

    private static final int MAX_TEXT = 500;

    private static final class PendingStep {
        String validationStatus;
        String validationDetail;
        String reflection;
        int attempts = 1;
    }

    private AgentRunContext() {
    }

    /** 开始一次运行（由编排器调用） */
    public static void begin() {
        BUFFER.set(new ArrayList<>());
        PENDING.remove();
    }

    public static boolean isActive() {
        return BUFFER.get() != null;
    }

    /** 标记本步的校验结果（由 BaseAgent 在 doExecute 之后调用） */
    public static void setValidation(String status, String detail) {
        PendingStep pending = pending();
        if (pending != null) {
            pending.validationStatus = status;
            pending.validationDetail = detail;
        }
    }

    /** 标记本步触发了反思重写（由子类在 doExecute 内调用） */
    public static void setReflection(String reflection, int attempts) {
        PendingStep pending = pending();
        if (pending != null) {
            pending.reflection = reflection == null ? null : truncate(reflection);
            pending.attempts = Math.max(1, attempts);
        }
    }

    /** 本步是否已经记录过反思（供子类避免重复触发） */
    public static boolean hasReflection() {
        PendingStep pending = pending();
        return pending != null && pending.reflection != null;
    }

    /** 记录一步（由 BaseAgent 调用）；不在一次运行中则忽略 */
    public static void record(String agentName, boolean success, long durationMs, Object output, String error) {
        List<StepRecord> buffer = BUFFER.get();
        if (buffer == null) {
            return;
        }
        PendingStep pending = pending();
        buffer.add(new StepRecord(
                agentName,
                success ? "SUCCESS" : "FAILED",
                durationMs,
                digest(output),
                truncate(error),
                success ? (pending != null && pending.validationStatus != null ? pending.validationStatus : "SKIP") : "SKIP",
                pending != null ? pending.validationDetail : null,
                pending != null ? pending.reflection : null,
                pending != null ? pending.attempts : 1,
                fields(output)));
        PENDING.remove();
    }

    /** 结束并取走本次运行的所有步骤（由编排器调用） */
    public static List<StepRecord> collectAndClear() {
        List<StepRecord> buffer = BUFFER.get();
        BUFFER.remove();
        PENDING.remove();
        return buffer == null ? List.of() : List.copyOf(buffer);
    }

    public static void clear() {
        BUFFER.remove();
        PENDING.remove();
    }

    private static PendingStep pending() {
        if (BUFFER.get() == null) {
            return null;
        }
        PendingStep pending = PENDING.get();
        if (pending == null) {
            pending = new PendingStep();
            PENDING.set(pending);
        }
        return pending;
    }

    /** 产出顶层字段清单（Map 的 key，逗号分隔，截断 300 字符） */
    private static String fields(Object output) {
        if (!(output instanceof Map<?, ?> map) || map.isEmpty()) {
            return null;
        }
        String joined = map.keySet().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        return joined.length() <= 300 ? joined : joined.substring(0, 299) + "…";
    }

    /** 输出摘要：只留前 500 字符，避免把整篇画像/计划塞进记录表 */
    private static String digest(Object output) {
        if (output == null) {
            return null;
        }
        try {
            return truncate(JsonUtils.toJsonString(output));
        } catch (Exception e) {
            return null;
        }
    }

    /** 截断到 MAX_TEXT 以内（预留 1 个字符给省略号，避免恰好超出列宽） */
    private static String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_TEXT ? text : text.substring(0, MAX_TEXT - 1) + "…";
    }
}
