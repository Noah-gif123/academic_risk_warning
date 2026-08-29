package com.example.academic_risk_warning.agent.core;

import com.example.academic_risk_warning.llm.LLMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 智能体基类：模板方法模式，统一处理日志、异常、超时
 *
 * @param <T> Agent 产出数据类型
 */
public abstract class BaseAgent<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** LLM 调用客户端 */
    protected LLMClient llmClient;

    /** 智能体名称 */
    protected String agentName;

    public BaseAgent(LLMClient llmClient, String agentName) {
        this.llmClient = llmClient;
        this.agentName = agentName;
    }

    /**
     * 智能体执行入口（模板方法）
     */
    public AgentResult<T> execute(AgentContext ctx) {
        long start = System.currentTimeMillis();
        log.info("[{}] 开始执行，学生ID={}", agentName, ctx.getStudentId());
        try {
            validate(ctx);
            T data = doExecute(ctx);
            long duration = System.currentTimeMillis() - start;
            log.info("[{}] 执行完成，耗时 {}ms", agentName, duration);

            AgentResult<T> result = AgentResult.success(agentName, data);
            result.setDurationMs(duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[{}] 执行失败，耗时 {}ms", agentName, duration, e);
            AgentResult<T> result = AgentResult.failure(agentName, e.getMessage());
            result.setDurationMs(duration);
            return result;
        }
    }

    /**
     * 输入校验，子类可覆盖
     */
    protected void validate(AgentContext ctx) {
        if (ctx == null || ctx.getStudentId() == null) {
            throw new IllegalArgumentException("AgentContext 或 studentId 不能为空");
        }
    }

    /**
     * 核心逻辑：子类必须实现
     */
    protected abstract T doExecute(AgentContext ctx);

    /**
     * 生成 System Prompt：子类必须实现
     */
    public abstract String getSystemPrompt();

    /**
     * 构建 User Prompt 上下文
     */
    public abstract String buildUserPrompt(AgentContext ctx);

    /**
     * 调用 LLM 生成回复的快捷方法
     */
    protected String callLLM(AgentContext ctx) {
        return llmClient.chat(getSystemPrompt(), buildUserPrompt(ctx));
    }

    /**
     * 带结构化要求调用 LLM
     */
    protected String callLLMStructured(AgentContext ctx, String outputSchema) {
        String prompt = buildUserPrompt(ctx)
                + "\n\n请严格按照以下 JSON 格式输出（不要包含 ```json 标记）：\n"
                + outputSchema;
        return llmClient.chat(getSystemPrompt(), prompt);
    }

    // ===== Getters =====

    public String getAgentName() { return agentName; }
}
