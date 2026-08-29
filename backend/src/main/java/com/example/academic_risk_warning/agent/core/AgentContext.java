package com.example.academic_risk_warning.agent.core;

import com.example.academic_risk_warning.vo.StudentFullVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体上下文：在流水线各阶段之间传递数据快照
 */
public class AgentContext {

    /** 学生ID */
    private final Long studentId;

    /** 课程ID */
    private Long courseId;

    /** 触发类型：SCHEDULED / MANUAL / ALERT_TRIGGERED */
    private String triggerType;

    /** 触发时间 */
    private LocalDateTime triggerTime;

    // ===== 学生原始数据（由调用方注入） =====
    private StudentFullVO studentFullVO;
    private List<Map<String, Object>> weakPoints;
    private Map<String, BigDecimal> riskScores;

    // ===== 各智能体产出 =====
    private Map<String, Object> monitorResult;
    private Map<String, Object> analysisResult;
    private Map<String, Object> profileResult;
    private Map<String, Object> recommendResult;
    private Map<String, Object> strategyResult;
    private Map<String, Object> feedbackResult;

    /**
     * 是否跳过 MonitorAgent 的数据持久化（用于 preloadContext 场景）
     */
    private boolean skipMonitorPersistence = false;

    public AgentContext(Long studentId) {
        this.studentId = studentId;
        this.triggerTime = LocalDateTime.now();
    }

    // ===== Getters & Setters =====

    public Long getStudentId() { return studentId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long v) { this.courseId = v; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String v) { this.triggerType = v; }

    public LocalDateTime getTriggerTime() { return triggerTime; }
    public void setTriggerTime(LocalDateTime v) { this.triggerTime = v; }

    public StudentFullVO getStudentFullVO() { return studentFullVO; }
    public void setStudentFullVO(StudentFullVO v) { this.studentFullVO = v; }

    public List<Map<String, Object>> getWeakPoints() { return weakPoints; }
    public void setWeakPoints(List<Map<String, Object>> v) { this.weakPoints = v; }

    public Map<String, BigDecimal> getRiskScores() { return riskScores; }
    public void setRiskScores(Map<String, BigDecimal> v) { this.riskScores = v; }

    public Map<String, Object> getMonitorResult() { return monitorResult; }
    public void setMonitorResult(Map<String, Object> v) { this.monitorResult = v; }

    public Map<String, Object> getAnalysisResult() { return analysisResult; }
    public void setAnalysisResult(Map<String, Object> v) { this.analysisResult = v; }

    public Map<String, Object> getProfileResult() { return profileResult; }
    public void setProfileResult(Map<String, Object> v) { this.profileResult = v; }

    public Map<String, Object> getRecommendResult() { return recommendResult; }
    public void setRecommendResult(Map<String, Object> v) { this.recommendResult = v; }

    public Map<String, Object> getStrategyResult() { return strategyResult; }
    public void setStrategyResult(Map<String, Object> v) { this.strategyResult = v; }

    public Map<String, Object> getFeedbackResult() { return feedbackResult; }
    public void setFeedbackResult(Map<String, Object> v) { this.feedbackResult = v; }

    public boolean isSkipMonitorPersistence() { return skipMonitorPersistence; }
    public void setSkipMonitorPersistence(boolean v) { this.skipMonitorPersistence = v; }

    // ===== 通用属性缓存（各 Agent 间传递临时数据） =====
    private final Map<String, Object> attributes = new HashMap<>();

    public void setAttribute(String key, Object value) { attributes.put(key, value); }
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) { return (T) attributes.get(key); }
    public boolean hasAttribute(String key) { return attributes.containsKey(key); }
}
