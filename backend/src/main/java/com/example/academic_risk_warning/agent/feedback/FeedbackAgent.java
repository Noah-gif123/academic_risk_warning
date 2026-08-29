package com.example.academic_risk_warning.agent.feedback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.core.BaseAgent;
import com.example.academic_risk_warning.agent.core.JsonUtils;
import com.example.academic_risk_warning.config.WarningSystemProperties;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.llm.BailianRAGClient;
import com.example.academic_risk_warning.llm.LLMClient;
import com.example.academic_risk_warning.mapper.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能体：反馈交互 — 承接学生反馈/答疑，联动策略调整和资源推送
 *
 * 核心能力：
 * 1. 意图识别：学术问题 / 正反馈 / 负反馈 / 建议 / 一般聊天
 * 2. RAG 答疑：课程知识问题走百炼知识库检索
 * 3. 反馈加工：提取反馈关键词、分类、情感极性，生成结构化反馈数据供 StrategyAgent 消费
 * 4. 多轮对话：保留上下文历史
 */
@Component
public class FeedbackAgent extends BaseAgent<Map<String, Object>> {

    private final StudentMapper studentMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final StudentWeakPointMapper weakPointMapper;
    private final KnowledgeMasteryMapper masteryMapper;
    private final WarningSystemProperties warningProps;
    private final BailianRAGClient bailianRAGClient;

    public FeedbackAgent(LLMClient llmClient,
                         StudentMapper studentMapper,
                         AlertRecordMapper alertRecordMapper,
                         StudentWeakPointMapper weakPointMapper,
                         KnowledgeMasteryMapper masteryMapper,
                         WarningSystemProperties warningProps,
                         BailianRAGClient bailianRAGClient) {
        super(llmClient, "FeedbackAgent");
        this.studentMapper = studentMapper;
        this.alertRecordMapper = alertRecordMapper;
        this.weakPointMapper = weakPointMapper;
        this.masteryMapper = masteryMapper;
        this.warningProps = warningProps;
        this.bailianRAGClient = bailianRAGClient;
    }

    // ===== System Prompt =====

    @Override
    public String getSystemPrompt() {
        return """
            你是学业预警系统的「学生交互智能体」，负责与学生对话、收集反馈和解答课程疑问。

            职责：
            1. 意图识别：分析学生消息属于以下哪一类
               - ACADEMIC_QUESTION：课程知识问题 → 调用知识库检索回答
               - POSITIVE_FEEDBACK：对学习计划/推荐内容表示满意
               - NEGATIVE_FEEDBACK：抱怨太难、太简单、不相关、节奏过快/过慢
               - SUGGESTION：对教学或系统提出建议
               - GENERAL：闲聊/其他

            2. 结构化反馈提取（FEEDBACK 类消息）：
               - 反馈维度：DIFFICULTY（难度）、RELEVANCE（相关性）、PACE（节奏）、QUALITY（质量）、OTHER
               - 方向：TOO_HARD / TOO_EASY / IRRELEVANT / TOO_FAST / TOO_SLOW / SATISFIED
               - 关键知识点（如有提及）
               - 情感值：-1.0（强烈不满）到 1.0（非常满意）

            3. 对话风格：友好、鼓励、共情，让学生感到被重视。

            请按以下 JSON 格式输出：
            {
              "intent": "ACADEMIC_QUESTION|POSITIVE_FEEDBACK|NEGATIVE_FEEDBACK|SUGGESTION|GENERAL",
              "reply": "给学生的回复文本",
              "sentiment": -1.0 到 1.0,
              "feedbackDetail": {
                "dimension": "DIFFICULTY|RELEVANCE|PACE|QUALITY|OTHER",
                "direction": "TOO_HARD|TOO_EASY|IRRELEVANT|TOO_FAST|TOO_SLOW|SATISFIED|NONE",
                "keywords": ["关键词1", "关键词2"],
                "summary": "反馈摘要（50字以内）"
              }
            }

            注意：如果是 ACADEMIC_QUESTION，reply 字段请留空（由 RAG 系统填充）。
            不要输出 ```json 标记。""";
    }

    @Override
    public String buildUserPrompt(AgentContext ctx) {
        Long studentId = ctx.getStudentId();
        Student student = studentMapper.selectById(studentId);
        String studentName = student != null ? student.getStudentName() : "未知";

        Map<String, Object> feedback = ctx.getFeedbackResult();
        String studentMessage = "";
        if (feedback != null) {
            Object msg = feedback.get("content");
            if (msg != null) studentMessage = msg.toString();
        }

        // 附加上下文信息帮助意图识别
        StringBuilder sb = new StringBuilder();
        sb.append("学生：").append(studentName).append("（ID=").append(studentId).append("）\n");
        sb.append("学生消息：").append(studentMessage).append("\n\n");

        // 附加上下文：当前学习状态
        AlertRecord latestAlert = queryLatestAlert(studentId);
        if (latestAlert != null) {
            sb.append("=== 当前学习状态（辅助理解学生语境） ===\n");
            sb.append("风险等级：").append(latestAlert.getAlertLevel()).append("\n");
            sb.append("风险分：").append(latestAlert.getRiskScore()).append("\n");
        }

        // 薄弱知识点
        List<StudentWeakPoint> weakPoints = queryWeakPoints(studentId, null);
        if (!weakPoints.isEmpty()) {
            sb.append("薄弱知识点：");
            sb.append(weakPoints.stream().limit(5)
                    .map(wp -> "ID=" + wp.getKpId() + "(错误率" + wp.getErrorRate() + "%)")
                    .collect(Collectors.joining("、")));
            sb.append("\n");
        }

        // 历史反馈
        if (feedback != null) {
            Object history = feedback.get("history");
            if (history instanceof List && !((List<?>) history).isEmpty()) {
                sb.append("\n=== 对话历史 ===\n");
                for (Object h : (List<?>) history) {
                    sb.append(h.toString()).append("\n");
                }
            }
        }

        return sb.toString();
    }

    // ===== 核心执行 =====

    @Override
    protected Map<String, Object> doExecute(AgentContext ctx) {
        Map<String, Object> feedback = ctx.getFeedbackResult();
        String studentMessage = "";
        if (feedback != null) {
            Object msg = feedback.get("content");
            if (msg != null) studentMessage = msg.toString().trim();
        }

        if (studentMessage.isEmpty()) {
            return buildFallbackResult("学生消息为空");
        }

        // 步骤 1：调用 LLM 做意图识别 + 反馈提取
        String llmOutput = callLLM(ctx);
        Map<String, Object> llmResult = JsonUtils.parse(llmOutput);
        if (llmResult.containsKey("raw")) {
            llmResult = Map.of("intent", "GENERAL", "reply", llmOutput,
                    "feedbackDetail", Map.of("summary", "无法解析"));
        }

        String intent = Objects.toString(llmResult.getOrDefault("intent", "GENERAL"), "GENERAL");
        String reply = Objects.toString(llmResult.getOrDefault("reply", ""), "");
        double sentiment = parseDouble(llmResult.get("sentiment"), 0.0);

        @SuppressWarnings("unchecked")
        Map<String, Object> feedbackDetail = llmResult.get("feedbackDetail") instanceof Map
                ? (Map<String, Object>) llmResult.get("feedbackDetail")
                : Map.of();

        // 步骤 2：如果是学术问题，走 RAG 检索（按课程路由）
        if ("ACADEMIC_QUESTION".equals(intent)) {
            try {
                Long courseId = ctx.getCourseId();
                String ragReply = bailianRAGClient.ragQuery(studentMessage, courseId);
                reply = ragReply;
            } catch (Exception e) {
                log.warn("[FeedbackAgent] RAG 检索失败，降级使用 LLM 回复: {}", e.getMessage());
                if (reply.isEmpty()) {
                    reply = "抱歉，暂时无法检索到相关知识。建议你向任课老师或者助教咨询这个问题。";
                }
            }
        }

        // 步骤 3：构建结构化结果（供 StrategyAgent 消费）
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("intent", intent);
        result.put("reply", reply);
        result.put("sentiment", sentiment);
        result.put("rawMessage", studentMessage);
        result.put("feedbackDetail", feedbackDetail);
        result.put("agent", agentName);
        result.put("timestamp", LocalDateTime.now().toString());

        // 步骤 4：如果是负反馈或建议，同时更新上下文中的反馈结果，以便策略链使用
        if ("NEGATIVE_FEEDBACK".equals(intent) || "SUGGESTION".equals(intent)) {
            Map<String, Object> enriched = new LinkedHashMap<>(feedback != null ? feedback : Map.of());
            enriched.putAll(result);
            enriched.put("requiresStrategyAdjust", true);
            ctx.setFeedbackResult(enriched);
        }

        return result;
    }

    // ===== 公开方法 =====

    /**
     * 课程答疑专用：直接从百炼知识库检索回答（按课程路由）
     */
    public String answerQuestion(String question, Long courseId) {
        if (question == null || question.isBlank()) {
            return "请输入你的问题。";
        }
        try {
            return bailianRAGClient.ragQuery(question, courseId);
        } catch (Exception e) {
            log.error("[FeedbackAgent] RAG 答疑失败", e);
            return "抱歉，知识库检索暂时不可用，请稍后再试。";
        }
    }

    /**
     * 课程答疑（不区分课程，使用默认知识库）
     */
    public String answerQuestion(String question) {
        return answerQuestion(question, null);
    }

    /**
     * 快速意图识别（不经过完整 LLM 流程）
     */
    public Map<String, Object> classifyIntent(String message, Long studentId) {
        AgentContext ctx = new AgentContext(studentId);
        ctx.setFeedbackResult(Map.of("content", message));
        String llmOutput = callLLM(ctx);
        Map<String, Object> result = JsonUtils.parse(llmOutput);
        if (result.containsKey("raw")) {
            return Map.of("intent", "GENERAL", "reply", llmOutput,
                    "feedbackDetail", Map.of("summary", "无法解析"));
        }
        return result;
    }

    /**
     * 收集学习计划反馈
     */
    public Map<String, Object> collectPlanFeedback(Long studentId, Long planId,
                                                     String feedbackType, String comment) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("planId", planId);
        result.put("feedbackType", feedbackType);
        result.put("comment", comment);
        result.put("requiresStrategyAdjust",
                "TOO_HARD".equalsIgnoreCase(feedbackType)
                        || "TOO_EASY".equalsIgnoreCase(feedbackType)
                        || "IRRELEVANT".equalsIgnoreCase(feedbackType));

        log.info("[FeedbackAgent] 收集到计划反馈: studentId={}, planId={}, type={}",
                studentId, planId, feedbackType);
        return result;
    }

    // ===== 数据查询 =====

    private AlertRecord queryLatestAlert(Long studentId) {
        return alertRecordMapper.selectOne(
                new LambdaQueryWrapper<AlertRecord>()
                        .eq(AlertRecord::getStudentId, studentId)
                        .orderByDesc(AlertRecord::getCreateTime)
                        .last("LIMIT 1"));
    }

    private List<StudentWeakPoint> queryWeakPoints(Long studentId, Long courseId) {
        var wrapper = new LambdaQueryWrapper<StudentWeakPoint>()
                .eq(StudentWeakPoint::getStudentId, studentId)
                .orderByDesc(StudentWeakPoint::getErrorRate);
        if (courseId != null) wrapper.eq(StudentWeakPoint::getCourseId, courseId);
        return weakPointMapper.selectList(wrapper);
    }

    private double parseDouble(Object v, double defaultValue) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try { return Double.parseDouble((String) v); }
            catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private Map<String, Object> buildFallbackResult(String reason) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("intent", "GENERAL");
        result.put("reply", "你好！我是你的学习助手，有什么可以帮你的吗？");
        result.put("sentiment", 0.0);
        result.put("feedbackDetail", Map.of("summary", reason));
        result.put("agent", agentName);
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }
}
