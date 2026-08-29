package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.agent.orchestrator.AgentOrchestrator;
import com.example.academic_risk_warning.vo.StudentFullVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 风险中心服务：封装多智能体编排调用 + 上下文增强 + 结果聚合
 *
 * 为 Controller 提供统一的门面，隐藏内部 Agent 编排细节
 */
@Service
public class RiskCenterService {

    private static final Logger log = LoggerFactory.getLogger(RiskCenterService.class);

    private final AgentOrchestrator orchestrator;
    private final StudentQueryService studentQueryService;
    private final StudentLoginService studentLoginService;

    public RiskCenterService(AgentOrchestrator orchestrator,
                             StudentQueryService studentQueryService,
                             StudentLoginService studentLoginService) {
        this.orchestrator = orchestrator;
        this.studentQueryService = studentQueryService;
        this.studentLoginService = studentLoginService;
    }

    // ===== 1. 完整评估 =====

    /**
     * 对学生执行完整六智能体评估流水线
     *
     * @return { studentInfo, pipelineResult, duration }
     */
    public Map<String, Object> runFullAssessment(Long studentId, Long courseId) {
        long start = System.currentTimeMillis();
        log.info("[RiskCenter] 开始完整评估，学生ID={}, 课程ID={}", studentId, courseId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("courseId", courseId);

        // 注入学生基本信息
        enrichStudentInfo(result, studentId);

        // 执行流水线（带超时保护）
        Map<String, Object> pipelineResult;
        try {
            pipelineResult = CompletableFuture
                    .supplyAsync(() -> orchestrator.executeFullPipeline(studentId, courseId))
                    .get(120, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("[RiskCenter] 完整评估超时，学生ID={}", studentId);
            pipelineResult = Map.of("success", false, "error", "评估超时（120秒），请稍后重试");
        } catch (Exception e) {
            log.error("[RiskCenter] 完整评估异常，学生ID={}", studentId, e);
            pipelineResult = Map.of("success", false, "error", "评估异常: " + e.getMessage());
        }

        result.put("pipelineResult", pipelineResult);
        result.put("durationMs", System.currentTimeMillis() - start);
        result.put("success", Boolean.TRUE.equals(pipelineResult.get("success")));
        result.put("timestamp", new Date());

        log.info("[RiskCenter] 完整评估完成，学生ID={}, 耗时={}ms", studentId, System.currentTimeMillis() - start);
        return result;
    }

    // ===== 2. 反馈处理 =====

    /**
     * 学生提交反馈，触发反馈→策略→重推荐流水线
     */
    public Map<String, Object> processFeedback(Long studentId, Long courseId, String message) {
        long start = System.currentTimeMillis();
        log.info("[RiskCenter] 开始处理反馈，学生ID={}, message={}", studentId, message);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);

        // 先分析反馈意图（轻量级，不触发全链）
        Map<String, Object> analysis = orchestrator.analyzeFeedback(studentId, message);
        result.put("feedbackAnalysis", analysis);

        String intent = Objects.toString(analysis.get("intent"), "GENERAL");

        // 学术问题 → RAG 答疑（不触发策略链）
        if ("ACADEMIC_QUESTION".equals(intent)) {
            String reply = Objects.toString(analysis.get("reply"), "");
            result.put("mode", "QA_ONLY");
            result.put("reply", reply);
            result.put("success", true);
            result.put("durationMs", System.currentTimeMillis() - start);
            return result;
        }

        // 需要策略调整的反馈 → 走完整反馈链（复用 analzyeFeedback 结果，避免重复 LLM 调用）
        if (Boolean.TRUE.equals(analysis.get("requiresStrategyAdjust"))
                || "NEGATIVE_FEEDBACK".equals(intent)
                || "SUGGESTION".equals(intent)) {

            enrichStudentInfo(result, studentId);

            Map<String, Object> pipelineResult;
            try {
                pipelineResult = CompletableFuture
                        .supplyAsync(() -> orchestrator.executeFeedbackPipelineWithAnalysis(
                                studentId, courseId, message, analysis))
                        .get(120, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("[RiskCenter] 反馈流水线异常，学生ID={}", studentId, e);
                pipelineResult = Map.of("success", false, "error", e.getMessage());
            }

            result.put("mode", "FEEDBACK_LOOP");
            result.put("pipelineResult", pipelineResult);
            result.put("adjusted", Objects.toString(
                    extractFromPipeline(pipelineResult, "strategy", "action"), "CONTINUE"));
        } else {
            // 正面反馈或通用消息 → 只返回友好回复
            String reply = Objects.toString(analysis.get("reply"),
                    "感谢你的反馈！我们已记录你的评价，继续加油！");
            result.put("mode", "ACK_ONLY");
            result.put("reply", reply);
        }

        result.put("success", true);
        result.put("durationMs", System.currentTimeMillis() - start);
        return result;
    }

    // ===== 3. 端到端闭环 =====

    /**
     * 端到端闭环：完整评估 + 反馈处理 + 策略调整 + 重推荐
     */
    public Map<String, Object> runCloseLoop(Long studentId, Long courseId, String feedback) {
        long start = System.currentTimeMillis();
        log.info("[RiskCenter] 开始端到端闭环，学生ID={}", studentId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        enrichStudentInfo(result, studentId);

        Map<String, Object> pipelineResult;
        try {
            pipelineResult = CompletableFuture
                    .supplyAsync(() -> orchestrator.executeCloseLoopPipeline(studentId, courseId, feedback))
                    .get(180, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            pipelineResult = Map.of("success", false, "error", "闭环流水线超时（180秒）");
        } catch (Exception e) {
            pipelineResult = Map.of("success", false, "error", "闭环异常: " + e.getMessage());
        }

        result.put("pipelineResult", pipelineResult);
        result.put("success", Boolean.TRUE.equals(pipelineResult.get("success")));
        result.put("durationMs", System.currentTimeMillis() - start);
        result.put("closeLoopCompleted", Boolean.TRUE.equals(pipelineResult.get("closeLoopCompleted")));

        return result;
    }

    // ===== 4. 效果驱动自动调整 =====

    /**
     * 基于效果数据的自动策略调整（无需学生反馈）
     * 用于定时任务或教师手动触发
     */
    public Map<String, Object> runEffectCheck(Long studentId, Long courseId) {
        long start = System.currentTimeMillis();
        log.info("[RiskCenter] 开始效果检查，学生ID={}, 课程ID={}", studentId, courseId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        enrichStudentInfo(result, studentId);

        Map<String, Object> pipelineResult;
        try {
            pipelineResult = orchestrator.executeEffectBasedAdjustment(studentId, courseId);
        } catch (Exception e) {
            log.error("[RiskCenter] 效果检查异常", e);
            pipelineResult = Map.of("success", false, "error", e.getMessage());
        }

        result.put("pipelineResult", pipelineResult);
        result.put("success", Boolean.TRUE.equals(pipelineResult.get("success")));
        result.put("adjustmentTriggered", Boolean.TRUE.equals(pipelineResult.get("adjustmentTriggered")));
        result.put("durationMs", System.currentTimeMillis() - start);

        return result;
    }

    // ===== 5. RAG 知识库问答 =====

    /**
     * 通过 RAG 知识库问答（由 FeedbackAgent 的 BailianRAGClient 提供）
     */
    public Map<String, Object> answerQuestion(String question) {
        return answerQuestion(question, null);
    }

    /**
     * 通过 RAG 知识库问答（支持课程级知识库路由）
     */
    public Map<String, Object> answerQuestion(String question, Long courseId) {
        long start = System.currentTimeMillis();
        try {
            String reply = orchestrator.answerQuestion(question, courseId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("reply", reply);
            result.put("courseId", courseId);
            result.put("durationMs", System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("[RiskCenter] RAG 问答异常", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "RAG 知识库检索失败: " + e.getMessage());
            return result;
        }
    }

    // ===== 6. 流水线统计 =====

    public Map<String, Object> getPipelineStats() {
        return orchestrator.getPipelineStats();
    }

    // ===== 7. 学习计划反馈快捷方法 =====

    public Map<String, Object> handlePlanFeedback(Long studentId, Long planId,
                                                    String feedbackType, String comment) {
        return orchestrator.handlePlanFeedback(studentId, planId, feedbackType, comment);
    }

    // ===== 辅助方法 =====

    /**
     * 从流水线结果中提取指定智能体的指定字段
     */
    @SuppressWarnings("unchecked")
    private Object extractFromPipeline(Map<String, Object> pipelineResult, String agent, String field) {
        if (pipelineResult == null) return null;
        Object agentResult = pipelineResult.get(agent);
        if (agentResult instanceof Map) {
            Map<String, Object> am = (Map<String, Object>) agentResult;
            Object data = am.get("data");
            if (data instanceof Map) {
                return ((Map<String, Object>) data).get(field);
            }
        }
        return null;
    }

    /**
     * 注入学生基本信息到结果 Map
     */
    private void enrichStudentInfo(Map<String, Object> result, Long studentId) {
        if (studentId == null) return;
        try {
            var entity = studentLoginService.getStudentById(studentId);
            if (entity != null) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("studentId", entity.getId());
                info.put("studentName", entity.getStudentName());
                info.put("studentNo", entity.getStudentNo());
                info.put("className", entity.getClassName());
                result.put("studentInfo", info);
            }
        } catch (Exception e) {
            log.warn("[RiskCenter] 获取学生信息失败，studentId={}", studentId);
        }
    }
}
