package com.example.academic_risk_warning.agent.orchestrator;

import com.example.academic_risk_warning.agent.analysis.AnalysisAgent;
import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.core.AgentEvent;
import com.example.academic_risk_warning.agent.core.AgentResult;
import com.example.academic_risk_warning.agent.core.AgentRunContext;
import com.example.academic_risk_warning.agent.feedback.FeedbackAgent;
import com.example.academic_risk_warning.agent.monitor.MonitorAgent;
import com.example.academic_risk_warning.agent.profile.ProfileAgent;
import com.example.academic_risk_warning.agent.recommend.RecommendAgent;
import com.example.academic_risk_warning.agent.strategy.StrategyAgent;
import com.example.academic_risk_warning.agent.tool.DataQueryAgent;
import com.example.academic_risk_warning.config.AgentProperties;
import com.example.academic_risk_warning.service.AgentRunService;
import com.example.academic_risk_warning.service.StudentMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 智能体协调器：编排六智能体完整调用链
 *
 * 三条核心流水线：
 * 1. 完整流水线：监测 → 分析 → 画像 → 推荐（初始评估）
 * 2. 反馈流水线：反馈 → 策略调整 → 重新推荐（反馈闭环）
 * 3. 闭环流水线：完整链 + 反馈采集 + 策略调整 + 重推荐（端到端）
 */
@Component
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final MonitorAgent monitorAgent;
    private final AnalysisAgent analysisAgent;
    private final ProfileAgent profileAgent;
    private final RecommendAgent recommendAgent;
    private final StrategyAgent strategyAgent;
    private final FeedbackAgent feedbackAgent;
    private final ApplicationEventPublisher eventPublisher;
    private final AgentRunService agentRunService;
    private final AgentProperties agentProperties;
    private final StudentMemoryService studentMemoryService;
    private final DataQueryAgent dataQueryAgent;

    public AgentOrchestrator(MonitorAgent monitorAgent,
                             AnalysisAgent analysisAgent,
                             ProfileAgent profileAgent,
                             RecommendAgent recommendAgent,
                             StrategyAgent strategyAgent,
                             FeedbackAgent feedbackAgent,
                             ApplicationEventPublisher eventPublisher,
                             AgentRunService agentRunService,
                             AgentProperties agentProperties,
                             StudentMemoryService studentMemoryService,
                             DataQueryAgent dataQueryAgent) {
        this.monitorAgent = monitorAgent;
        this.analysisAgent = analysisAgent;
        this.profileAgent = profileAgent;
        this.recommendAgent = recommendAgent;
        this.strategyAgent = strategyAgent;
        this.feedbackAgent = feedbackAgent;
        this.eventPublisher = eventPublisher;
        this.agentRunService = agentRunService;
        this.agentProperties = agentProperties;
        this.studentMemoryService = studentMemoryService;
        this.dataQueryAgent = dataQueryAgent;
    }

    /**
     * 统一执行一次流水线，并把各步明细落库到 {@code agent_run / agent_run_step}。
     * 配置开关 {@code agent.enabled=false} 时直接返回"已关闭"，不调用任何智能体。
     */
    private Map<String, Object> withRun(Long studentId, Long courseId, String pipeline, String triggerType,
                                        Supplier<Map<String, Object>> body) {
        AgentRunContext.begin();
        try {
            Map<String, Object> result = body.get();
            Long runId = trySaveRun(studentId, courseId, pipeline, triggerType, AgentRunContext.collectAndClear());
            if (runId != null) result.put("runId", runId);
            // W3：流水线跑完刷新长期记忆（供下一次注入 prompt），失败不影响结果
            try {
                studentMemoryService.refreshMemory(studentId, courseId, runId);
            } catch (Exception e) {
                log.warn("[Orchestrator] 记忆刷新失败（不影响流水线结果）: {}", e.getMessage());
            }
            return result;
        } catch (RuntimeException e) {
            // 异常也要留痕：把已经跑过的步骤存下来，便于排查
            List<AgentRunContext.StepRecord> steps = AgentRunContext.collectAndClear();
            if (!steps.isEmpty()) {
                trySaveRun(studentId, courseId, pipeline, triggerType, steps);
            }
            throw e;
        }
    }

    /** 记录运行明细；写记录失败绝不能影响业务结果，因此这里只记日志 */
    private Long trySaveRun(Long studentId, Long courseId, String pipeline, String triggerType,
                            List<AgentRunContext.StepRecord> steps) {
        try {
            return agentRunService.saveRun(studentId, courseId, pipeline, triggerType, steps);
        } catch (Exception e) {
            log.error("[Orchestrator] 运行记录写入失败（不影响流水线结果）: {}", e.getMessage());
            return null;
        }
    }

    /** MAS 关闭时的统一返回（不触发任何 LLM 调用） */
    private Map<String, Object> disabledResult(String pipeline) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("disabled", true);
        result.put("pipeline", pipeline);
        result.put("message", "多智能体功能已关闭（agent.enabled=false），本次未执行任何智能体");
        return result;
    }

    // ===== 流水线 1：完整评估流水线 =====

    /**
     * 完整流水线：监测 → 分析 → 画像 → 推荐
     * 用于首次评估或手动触发全链路分析
     */
    public Map<String, Object> executeFullPipeline(Long studentId) {
        return executeFullPipeline(studentId, null);
    }

    @Transactional
    public Map<String, Object> executeFullPipeline(Long studentId, Long courseId) {
        if (!agentProperties.isEnabled()) return disabledResult("FULL");
        return withRun(studentId, courseId, "FULL", "MANUAL", () -> doExecuteFullPipeline(studentId, courseId));
    }

    /** 完整流水线主体：监测 → 分析 → 画像 → 推荐 */
    private Map<String, Object> doExecuteFullPipeline(Long studentId, Long courseId) {
        log.info("开始执行完整流水线，学生ID={}, 课程ID={}", studentId, courseId);
        AgentContext ctx = new AgentContext(studentId);
        ctx.setCourseId(courseId);
        ctx.setTriggerType("MANUAL");
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> succeeded = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        // 1. 学情监测
        AgentResult<Map<String, Object>> monitorR = monitorAgent.execute(ctx);
        if (monitorR.isSuccess()) {
            ctx.setMonitorResult(monitorR.getData());
            eventPublisher.publishEvent(new AgentEvent("MONITOR_COMPLETED", ctx, "MonitorAgent"));
            succeeded.add("monitor");
        } else {
            failed.add("monitor");
            log.warn("[Orchestrator] MonitorAgent 失败: {}", monitorR.getErrorMessage());
        }
        result.put("monitor", buildAgentResult(monitorR));

        // 2. 学习分析
        AgentResult<Map<String, Object>> analysisR = analysisAgent.execute(ctx);
        if (analysisR.isSuccess()) {
            ctx.setAnalysisResult(analysisR.getData());
            eventPublisher.publishEvent(new AgentEvent("ANALYSIS_COMPLETED", ctx, "AnalysisAgent"));
            succeeded.add("analysis");
        } else {
            failed.add("analysis");
            log.warn("[Orchestrator] AnalysisAgent 失败: {}", analysisR.getErrorMessage());
        }
        result.put("analysis", buildAgentResult(analysisR));

        // 3. 学情画像
        AgentResult<Map<String, Object>> profileR = profileAgent.execute(ctx);
        if (profileR.isSuccess()) {
            ctx.setProfileResult(profileR.getData());
            eventPublisher.publishEvent(new AgentEvent("PROFILE_COMPLETED", ctx, "ProfileAgent"));
            succeeded.add("profile");
        } else {
            failed.add("profile");
            log.warn("[Orchestrator] ProfileAgent 失败: {}", profileR.getErrorMessage());
        }
        result.put("profile", buildAgentResult(profileR));

        // 4. 资源推荐
        AgentResult<Map<String, Object>> recommendR = recommendAgent.execute(ctx);
        if (recommendR.isSuccess()) {
            ctx.setRecommendResult(recommendR.getData());
            succeeded.add("recommend");
        } else {
            failed.add("recommend");
            log.warn("[Orchestrator] RecommendAgent 失败: {}", recommendR.getErrorMessage());
        }
        result.put("recommend", buildAgentResult(recommendR));

        result.put("success", failed.isEmpty());
        result.put("succeededAgents", succeeded);
        result.put("failedAgents", failed);
        result.put("pipeline", "FULL");
        log.info("完整流水线执行完成，学生ID={}，成功={}，失败={}", studentId, succeeded.size(), failed.size());
        return result;
    }

    // ===== 流水线 2：反馈闭环流水线 =====

    /**
     * 反馈流水线：采集学生反馈 → 分析 → 策略调整 → 重新推荐
     * 用于学生提交反馈后触发调整
     */
    public Map<String, Object> executeFeedbackPipeline(Long studentId, String feedback) {
        return executeFeedbackPipeline(studentId, null, feedback);
    }

    @Transactional
    public Map<String, Object> executeFeedbackPipeline(Long studentId, Long courseId, String feedback) {
        if (!agentProperties.isEnabled()) return disabledResult("FEEDBACK");
        return withRun(studentId, courseId, "FEEDBACK", "FEEDBACK",
                () -> doExecuteFeedbackPipeline(studentId, courseId, feedback));
    }

    /** 反馈流水线主体：反馈分析 → 策略调整 → 重推荐 */
    private Map<String, Object> doExecuteFeedbackPipeline(Long studentId, Long courseId, String feedback) {
        log.info("开始执行反馈流水线，学生ID={}", studentId);
        AgentContext ctx = new AgentContext(studentId);
        ctx.setCourseId(courseId);
        ctx.setTriggerType("FEEDBACK");

        // 预加载背景数据
        preloadContext(ctx);

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> succeeded = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        // 1. 反馈分析
        ctx.setFeedbackResult(Map.of("content", feedback));
        AgentResult<Map<String, Object>> feedbackR = feedbackAgent.execute(ctx);
        Map<String, Object> fbData;
        if (feedbackR.isSuccess()) {
            fbData = feedbackR.getData();
            ctx.setFeedbackResult(fbData); // 用 LLM 分析后的结果覆盖
            succeeded.add("feedback");
            log.info("[Orchestrator] 反馈分析完成，意图={}", fbData.get("intent"));
        } else {
            ctx.setFeedbackResult(Map.of("content", feedback, "requiresStrategyAdjust", true));
            failed.add("feedback");
            fbData = Map.of("content", feedback);
        }
        result.put("feedback", buildAgentResult(feedbackR));

        // 2. 如果是学术问题，直接返回答案（不需要调整策略）
        String intent = Objects.toString(fbData.get("intent"), "");
        if ("ACADEMIC_QUESTION".equals(intent)) {
            result.put("success", true);
            result.put("pipeline", "QA_ONLY");
            result.put("agentChain", List.of("feedback"));
            return result;
        }

        // 3. 策略调整
        AgentResult<Map<String, Object>> strategyR = strategyAgent.execute(ctx);
        if (strategyR.isSuccess()) {
            ctx.setStrategyResult(strategyR.getData());
            eventPublisher.publishEvent(new AgentEvent("STRATEGY_ADJUSTED", ctx, "StrategyAgent"));
            succeeded.add("strategy");
        } else {
            failed.add("strategy");
        }
        result.put("strategy", buildAgentResult(strategyR));

        // 4. 基于新策略重新推荐
        AgentResult<Map<String, Object>> recommendR = recommendAgent.execute(ctx);
        if (recommendR.isSuccess()) {
            ctx.setRecommendResult(recommendR.getData());
            succeeded.add("recommend");
        } else {
            failed.add("recommend");
        }
        result.put("recommend", buildAgentResult(recommendR));

        result.put("success", failed.isEmpty());
        result.put("succeededAgents", succeeded);
        result.put("failedAgents", failed);
        result.put("pipeline", "FEEDBACK");
        log.info("反馈流水线执行完成，学生ID={}，成功={}，失败={}", studentId, succeeded.size(), failed.size());
        return result;
    }

    /**
     * 反馈流水线（复用已有的反馈分析结果，避免重复 LLM 调用）
     * 与 processFeedback 联动使用：analyzeFeedback 的结果直接传入
     */
    @Transactional
    public Map<String, Object> executeFeedbackPipelineWithAnalysis(Long studentId, Long courseId,
                                                                     String feedback,
                                                                     Map<String, Object> preAnalysis) {
        log.info("开始执行反馈流水线（复用预分析），学生ID={}", studentId);
        AgentContext ctx = new AgentContext(studentId);
        ctx.setCourseId(courseId);
        ctx.setTriggerType("FEEDBACK");

        preloadContext(ctx);

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> succeeded = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        // 复用预分析结果，跳过 FeedbackAgent 的 LLM 调用
        Map<String, Object> fbData = new LinkedHashMap<>(preAnalysis);
        ctx.setFeedbackResult(fbData);
        succeeded.add("feedback");
        result.put("feedback", Map.of("success", true, "data", fbData, "agent", "FeedbackAgent", "durationMs", 0));

        String intent = Objects.toString(fbData.get("intent"), "");
        if ("ACADEMIC_QUESTION".equals(intent)) {
            result.put("success", true);
            result.put("pipeline", "QA_ONLY");
            result.put("agentChain", List.of("feedback"));
            return result;
        }

        // 策略调整 + 重推荐（与原方法相同）
        AgentResult<Map<String, Object>> strategyR = strategyAgent.execute(ctx);
        if (strategyR.isSuccess()) {
            ctx.setStrategyResult(strategyR.getData());
            eventPublisher.publishEvent(new AgentEvent("STRATEGY_ADJUSTED", ctx, "StrategyAgent"));
            succeeded.add("strategy");
        } else {
            failed.add("strategy");
        }
        result.put("strategy", buildAgentResult(strategyR));

        AgentResult<Map<String, Object>> recommendR = recommendAgent.execute(ctx);
        if (recommendR.isSuccess()) {
            ctx.setRecommendResult(recommendR.getData());
            succeeded.add("recommend");
        } else {
            failed.add("recommend");
        }
        result.put("recommend", buildAgentResult(recommendR));

        result.put("success", failed.isEmpty());
        result.put("succeededAgents", succeeded);
        result.put("failedAgents", failed);
        result.put("pipeline", "FEEDBACK");
        log.info("反馈流水线（复用预分析）执行完成，学生ID={}", studentId);
        return result;
    }

    // ===== 流水线 3：端到端闭环流水线 =====

    /**
     * 端到端闭环流水线：完整评估 + 模拟反馈 + 策略调整 + 重推荐
     * 演示完整的 "监测 → 分析 → 画像 → 推荐 → 反馈 → 调整 → 重推" 闭环
     */
    @Transactional
    public Map<String, Object> executeCloseLoopPipeline(Long studentId, Long courseId,
                                                          String simulatedFeedback) {
        log.info("开始执行端到端闭环流水线，学生ID={}, 课程ID={}", studentId, courseId);

        Map<String, Object> result = new LinkedHashMap<>();

        // 阶段 1：完整评估
        Map<String, Object> fullResult = executeFullPipeline(studentId, courseId);
        result.put("phase1_evaluation", fullResult);

        // 阶段 2：反馈闭环（使用模拟或真实反馈）
        if (simulatedFeedback != null && !simulatedFeedback.isBlank()) {
            Map<String, Object> feedbackResult = executeFeedbackPipeline(studentId, courseId, simulatedFeedback);
            result.put("phase2_feedbackLoop", feedbackResult);
            result.put("closeLoopCompleted", true);
        } else {
            result.put("closeLoopCompleted", false);
            result.put("note", "未提供模拟反馈，跳过了反馈闭环阶段");
        }

        result.put("success", Boolean.TRUE.equals(fullResult.get("success")));
        result.put("pipeline", "CLOSE_LOOP");
        log.info("端到端闭环流水线执行完成，学生ID={}", studentId);
        return result;
    }

    // ===== 流水线 4：效果评估 + 自动调整 =====

    /**
     * 基于效果数据的自动策略调整（无需学生主动反馈）
     * 用于定时任务触发：检查练习完成率和得分，自动调整
     */
    @Transactional
    public Map<String, Object> executeEffectBasedAdjustment(Long studentId, Long courseId) {
        if (!agentProperties.isEnabled()) return disabledResult("EFFECT_BASED");
        return withRun(studentId, courseId, "EFFECT_BASED", "EFFECT_CHECK",
                () -> doExecuteEffectBasedAdjustment(studentId, courseId));
    }

    /** 效果驱动调整主体：策略分析 → （必要时）重推荐 */
    private Map<String, Object> doExecuteEffectBasedAdjustment(Long studentId, Long courseId) {
        log.info("开始执行效果驱动的策略调整，学生ID={}", studentId);
        AgentContext ctx = new AgentContext(studentId);
        ctx.setCourseId(courseId);
        ctx.setTriggerType("EFFECT_CHECK");

        // 预加载上下文
        preloadContext(ctx);

        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 策略分析（基于效果数据，无学生反馈）
        AgentResult<Map<String, Object>> strategyR = strategyAgent.execute(ctx);
        String action = "CONTINUE";
        if (strategyR.isSuccess()) {
            ctx.setStrategyResult(strategyR.getData());
            action = Objects.toString(strategyR.getData().get("action"), "CONTINUE");
            eventPublisher.publishEvent(new AgentEvent("STRATEGY_ADJUSTED", ctx, "StrategyAgent"));
        }
        result.put("strategy", buildAgentResult(strategyR));

        // 2. 如果策略需要调整，触发重推荐
        if (!"CONTINUE".equals(action)) {
            AgentResult<Map<String, Object>> recommendR = recommendAgent.execute(ctx);
            if (recommendR.isSuccess()) {
                ctx.setRecommendResult(recommendR.getData());
            }
            result.put("recommend", buildAgentResult(recommendR));
            result.put("adjustmentTriggered", true);
        } else {
            result.put("adjustmentTriggered", false);
            result.put("note", "当前效果良好，无需调整");
        }

        result.put("success", true);
        result.put("pipeline", "EFFECT_BASED");
        log.info("效果驱动调整完成，学生ID={}，决策={}", studentId, action);
        return result;
    }

    // ===== 快捷方法 =====

    /**
     * 答疑模式：直接调用 RAG 知识库检索（不区分课程）
     */
    public String answerQuestion(String question) {
        return feedbackAgent.answerQuestion(question);
    }

    /**
     * 答疑模式：按课程路由到对应知识库检索
     */
    public String answerQuestion(String question, Long courseId) {
        return feedbackAgent.answerQuestion(question, courseId);
    }

    /**
     * 答疑 + 引用依据（W3）
     */
    public com.example.academic_risk_warning.llm.BailianRAGClient.RagAnswer answerQuestionDetailed(String question, Long courseId) {
        return feedbackAgent.answerQuestionDetailed(question, courseId);
    }

    /**
     * 工具调用问答（W3）：教师用自然语言问学情，DataQueryAgent 自主调用查询工具后作答。
     * 走统一的 withRun 留痕，教师端"运行历史"里能看到 TOOL_QA 这条流水线调了哪些工具。
     */
    public Map<String, Object> askWithTools(Long studentId, Long courseId, String question) {
        if (!agentProperties.isEnabled()) return disabledResult("TOOL_QA");
        return withRun(studentId, courseId, "TOOL_QA", "QA", () -> {
            AgentContext ctx = new AgentContext(studentId);
            ctx.setCourseId(courseId);
            ctx.setAttribute("question", question);

            AgentResult<Map<String, Object>> r = dataQueryAgent.execute(ctx);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", r.isSuccess());
            result.put("pipeline", "TOOL_QA");
            if (r.isSuccess()) {
                result.putAll(r.getData());
            } else {
                result.put("message", r.getErrorMessage());
            }
            result.put("durationMs", r.getDurationMs());
            return result;
        });
    }

    /**
     * 单步反馈分析（不触发策略链）
     */    public Map<String, Object> analyzeFeedback(Long studentId, String message) {
        AgentContext ctx = new AgentContext(studentId);
        ctx.setFeedbackResult(Map.of("content", message));
        AgentResult<Map<String, Object>> r = feedbackAgent.execute(ctx);
        return r.isSuccess() ? r.getData() : Map.of("intent", "GENERAL", "reply", r.getErrorMessage());
    }

    /**
     * 收集学习计划反馈 + 策略调整 + 重推荐（快捷方法）
     */
    public Map<String, Object> handlePlanFeedback(Long studentId, Long planId,
                                                    String feedbackType, String comment) {
        // 先用 FeedbackAgent 结构化反馈
        Map<String, Object> fbResult = feedbackAgent.collectPlanFeedback(studentId, planId, feedbackType, comment);

        // 如果不需要调整，直接返回
        if (!Boolean.TRUE.equals(fbResult.get("requiresStrategyAdjust"))) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("feedback", fbResult);
            result.put("adjustmentTriggered", false);
            result.put("reply", "感谢你的反馈！当前学习计划看起来很适合你，继续加油！");
            result.put("success", true);
            return result;
        }

        // 需要调整：走反馈闭环
        String feedbackMsg = buildFeedbackMessage(feedbackType, comment);
        return executeFeedbackPipeline(studentId, null, feedbackMsg);
    }

    // ===== 辅助方法 =====

    /**
     * 预加载上下文数据（供反馈/效果检查流水线使用）
     */
    private void preloadContext(AgentContext ctx) {
        try {
            // 跳过持久化：仅采集最新监测数据，不写入 MonitorReport
            ctx.setSkipMonitorPersistence(true);
            AgentResult<Map<String, Object>> monitorR = monitorAgent.execute(ctx);
            ctx.setSkipMonitorPersistence(false);
            if (monitorR.isSuccess()) {
                ctx.setMonitorResult(monitorR.getData());
            }
        } catch (Exception e) {
            log.warn("[Orchestrator] 预加载监测数据失败: {}", e.getMessage());
        }
    }

    private String buildFeedbackMessage(String feedbackType, String comment) {
        StringBuilder sb = new StringBuilder();
        switch (feedbackType.toUpperCase()) {
            case "TOO_HARD" -> sb.append("我觉得当前的练习太难了。");
            case "TOO_EASY" -> sb.append("我觉得当前的练习太简单了，没有挑战性。");
            case "IRRELEVANT" -> sb.append("推荐的练习内容和我的薄弱点不太相关。");
            case "TOO_FAST" -> sb.append("学习节奏太快了，跟不上。");
            case "TOO_SLOW" -> sb.append("学习节奏太慢了。");
            default -> sb.append("我对当前学习计划有反馈。");
        }
        if (comment != null && !comment.isBlank()) {
            sb.append(" ").append(comment);
        }
        return sb.toString();
    }

    private Map<String, Object> buildAgentResult(AgentResult<?> r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", r.isSuccess());
        if (r.isSuccess()) {
            m.put("data", r.getData());
        } else {
            m.put("error", r.getErrorMessage());
        }
        m.put("agent", r.getAgentName());
        m.put("durationMs", r.getDurationMs());
        return m;
    }

    /**
     * 获取流水线统计数据
     */
    public Map<String, Object> getPipelineStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("availableAgents", List.of(
                "MonitorAgent", "AnalysisAgent", "ProfileAgent",
                "RecommendAgent", "StrategyAgent", "FeedbackAgent", "DataQueryAgent"));
        stats.put("pipelines", Map.of(
                "FULL", "监测→分析→画像→推荐",
                "FEEDBACK", "反馈→策略调整→重推荐",
                "CLOSE_LOOP", "完整链+反馈闭环",
                "EFFECT_BASED", "效果驱动自动调整",
                "TOOL_QA", "工具调用问答（自然语言查学情）",
                "QA_ONLY", "RAG答疑"));
        stats.put("enabled", agentProperties.isEnabled());
        // 真实运行统计：成功率、各智能体失败率与平均耗时、P95 耗时、最近运行
        stats.putAll(agentRunService.getStats(5));
        return stats;
    }
}
