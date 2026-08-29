package com.example.academic_risk_warning.agent.core;

import java.time.LocalDateTime;

/**
 * 智能体间事件，用于松耦合通信
 */
public class AgentEvent {

    /** MONITOR_COMPLETED / ANALYSIS_COMPLETED / PROFILE_COMPLETED / FEEDBACK_RECEIVED */
    private final String eventType;

    /** 事件携带的上下文数据 */
    private final AgentContext context;

    /** 来源智能体名称 */
    private final String sourceAgent;

    /** 目标智能体名称（可为空，由协调器路由） */
    private String targetAgent;

    /** 事件时间 */
    private final LocalDateTime eventTime;

    public AgentEvent(String eventType, AgentContext context, String sourceAgent) {
        this.eventType = eventType;
        this.context = context;
        this.sourceAgent = sourceAgent;
        this.eventTime = LocalDateTime.now();
    }

    // ===== Getters =====

    public String getEventType() { return eventType; }

    public AgentContext getContext() { return context; }

    public String getSourceAgent() { return sourceAgent; }

    public String getTargetAgent() { return targetAgent; }
    public void setTargetAgent(String v) { this.targetAgent = v; }

    public LocalDateTime getEventTime() { return eventTime; }

    @Override
    public String toString() {
        return "AgentEvent{" + eventType + " from " + sourceAgent +
                " -> " + (targetAgent != null ? targetAgent : "ORCHESTRATOR") + "}";
    }
}
