package com.example.academic_risk_warning.agent.core;

import java.time.LocalDateTime;

/**
 * 智能体执行结果
 *
 * @param <T> 产出数据类型
 */
public class AgentResult<T> {

    /** 智能体名称 */
    private String agentName;

    /** 是否成功 */
    private boolean success;

    /** 错误信息（失败时） */
    private String errorMessage;

    /** 产出数据 */
    private T data;

    /** 执行耗时（毫秒） */
    private long durationMs;

    /** 执行时间 */
    private LocalDateTime executeTime;

    public static <T> AgentResult<T> success(String agentName, T data) {
        AgentResult<T> r = new AgentResult<>();
        r.agentName = agentName;
        r.success = true;
        r.data = data;
        r.executeTime = LocalDateTime.now();
        return r;
    }

    public static <T> AgentResult<T> failure(String agentName, String errorMessage) {
        AgentResult<T> r = new AgentResult<>();
        r.agentName = agentName;
        r.success = false;
        r.errorMessage = errorMessage;
        r.executeTime = LocalDateTime.now();
        return r;
    }

    // ===== Getters & Setters =====

    public String getAgentName() { return agentName; }
    public void setAgentName(String v) { this.agentName = v; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean v) { this.success = v; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }

    public T getData() { return data; }
    public void setData(T v) { this.data = v; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long v) { this.durationMs = v; }

    public LocalDateTime getExecuteTime() { return executeTime; }
    public void setExecuteTime(LocalDateTime v) { this.executeTime = v; }
}
