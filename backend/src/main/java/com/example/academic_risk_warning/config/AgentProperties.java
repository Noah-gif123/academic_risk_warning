package com.example.academic_risk_warning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * MAS 多智能体系统配置参数
 */
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    /** 全局开关：是否启用 MAS 智能体 */
    private boolean enabled = true;

    /** LLM 调用超时（秒） */
    private int llmTimeout = 120;

    /** 单个智能体执行超时（秒） */
    private int agentTimeout = 60;

    /** 百炼应用 API Key */
    private String bailianApiKey = "";

    /** 百炼知识库应用 ID（默认，未匹配到课程时兜底使用） */
    private String bailianAppId = "1aabcd737bb44364bd435642c22441e0";

    /**
     * 课程级知识库应用 ID 映射（courseId → appId）
     * 每门课程独立知识库，实现课程级知识隔离检索
     * 示例：agent.bailian-app-ids.1=xxx  (courseId=1)
     *       agent.bailian-app-ids.2=yyy  (courseId=2)
     */
    private Map<Long, String> bailianAppIds = new HashMap<>();

    /** 百炼 API 地址 */
    private String bailianBaseUrl = "https://dashscope.aliyuncs.com";

    /** RAG 召回数量 */
    private int ragTopK = 10;

    /** RAG 相似度阈值 */
    private double ragThreshold = 0.20;

    /** 是否启用"反思重写"（Self-Refine）：校验不通过时让模型带着批评再写一版 */
    private boolean reflectionEnabled = true;

    /** 反思重写最大轮次 */
    private int reflectionMaxRounds = 1;

    /** 工具调用最大轮次（W3：模型"查数据→再查→作答"的循环上限） */
    private int toolMaxRounds = 3;

    /**
     * RAG 是否请求应用的思考过程（W3 引用溯源）：
     * 开启后能拿到应用内部检索命中，用于展示"回答依据"；代价是 token 消耗更高。
     */
    private boolean ragThoughtsEnabled = false;

    // ==================== getters / setters ====================

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }

    public int getLlmTimeout() { return llmTimeout; }
    public void setLlmTimeout(int v) { this.llmTimeout = v; }

    public int getAgentTimeout() { return agentTimeout; }
    public void setAgentTimeout(int v) { this.agentTimeout = v; }

    public String getBailianApiKey() { return bailianApiKey; }
    public void setBailianApiKey(String v) { this.bailianApiKey = v; }

    public String getBailianAppId() { return bailianAppId; }
    public void setBailianAppId(String v) { this.bailianAppId = v; }

    public Map<Long, String> getBailianAppIds() { return bailianAppIds; }
    public void setBailianAppIds(Map<Long, String> v) { this.bailianAppIds = v; }

    /**
     * 根据 courseId 获取对应的百炼应用 ID
     * 如果指定课程有独立知识库则使用，否则回退到默认应用
     */
    public String getBailianAppId(Long courseId) {
        if (courseId != null && bailianAppIds != null && bailianAppIds.containsKey(courseId)) {
            return bailianAppIds.get(courseId);
        }
        return bailianAppId;
    }

    public String getBailianBaseUrl() { return bailianBaseUrl; }
    public void setBailianBaseUrl(String v) { this.bailianBaseUrl = v; }

    public int getRagTopK() { return ragTopK; }
    public void setRagTopK(int v) { this.ragTopK = v; }

    public double getRagThreshold() { return ragThreshold; }
    public void setRagThreshold(double v) { this.ragThreshold = v; }

    public boolean isReflectionEnabled() { return reflectionEnabled; }
    public void setReflectionEnabled(boolean v) { this.reflectionEnabled = v; }

    public int getReflectionMaxRounds() { return reflectionMaxRounds; }
    public void setReflectionMaxRounds(int v) { this.reflectionMaxRounds = v; }

    public int getToolMaxRounds() { return toolMaxRounds; }
    public void setToolMaxRounds(int v) { this.toolMaxRounds = v; }

    public boolean isRagThoughtsEnabled() { return ragThoughtsEnabled; }
    public void setRagThoughtsEnabled(boolean v) { this.ragThoughtsEnabled = v; }
}
