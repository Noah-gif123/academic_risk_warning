package com.example.academic_risk_warning.llm;

import java.util.Map;

/**
 * 统一 LLM 调用接口，屏蔽不同模型服务商的差异
 */
public interface LLMClient {

    /**
     * 通用对话：发送 prompt 并返回模型生成的文本
     *
     * @param systemPrompt 系统角色提示词
     * @param userPrompt   用户输入
     * @return 模型回复文本
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 带上下文对话
     *
     * @param systemPrompt 系统角色提示词
     * @param messages     历史消息列表，每条包含 role/user/content
     * @return 模型回复文本
     */
    String chat(String systemPrompt, java.util.List<Map<String, Object>> messages);

    /**
     * RAG 增强问答：先检索知识库再生成回答（不区分课程，使用默认知识库）
     *
     * @param question 用户问题
     * @return 基于知识库的回答
     */
    String ragQuery(String question);

    /**
     * RAG 增强问答：按课程检索对应的知识库后生成回答
     * 每门课程有独立的知识库应用，实现课程级知识隔离
     *
     * @param question 用户问题
     * @param courseId 课程ID，用于路由到对应课程的知识库应用
     * @return 基于该课程知识库的回答
     */
    default String ragQuery(String question, Long courseId) {
        // 默认实现：忽略 courseId，回退到通用 ragQuery
        return ragQuery(question);
    }
}
