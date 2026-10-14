package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.service.AgentEvalService;
import com.example.academic_risk_warning.service.AgentRunService;
import com.example.academic_risk_warning.service.RiskCenterService;
import com.example.academic_risk_warning.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 多智能体 API：暴露四条编排流水线 + RAG 知识库问答
 *
 * 六智能体：Monitor → Analysis → Profile → Recommend → Feedback → Strategy
 * 四条流水线：FULL / FEEDBACK / CLOSE_LOOP / EFFECT_BASED
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final RiskCenterService riskCenterService;
    private final TokenService tokenService;
    private final AgentRunService agentRunService;
    private final AgentEvalService agentEvalService;

    public AgentController(RiskCenterService riskCenterService,
                           TokenService tokenService,
                           AgentRunService agentRunService,
                           AgentEvalService agentEvalService) {
        this.riskCenterService = riskCenterService;
        this.tokenService = tokenService;
        this.agentRunService = agentRunService;
        this.agentEvalService = agentEvalService;
    }

    // ===== 流水线 1：完整评估 =====

    /**
     * POST /api/agent/full-assessment/{studentId}
     * Body: { "courseId": 1 }
     *
     * 完整流水线：监测 → 分析 → 画像 → 推荐
     * 用于教师查看某学生的完整智能体分析结果
     */
    @PostMapping("/full-assessment/{studentId}")
    public Map<String, Object> fullAssessment(@PathVariable Long studentId,
                                               @RequestBody(required = false) Map<String, Object> body,
                                               HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        Long courseId = body != null && body.get("courseId") != null
                ? Long.valueOf(body.get("courseId").toString()) : null;

        try {
            Map<String, Object> result = riskCenterService.runFullAssessment(studentId, courseId);
            result.put("message", "完整评估完成");
            return wrapSuccess(result);
        } catch (Exception e) {
            log.error("[AgentAPI] 完整评估失败，studentId={}", studentId, e);
            return wrapError("完整评估失败: " + e.getMessage());
        }
    }

    // ===== 流水线 2：反馈处理 =====

    /**
     * POST /api/agent/feedback
     * Body: { "studentId": 1, "courseId": 2, "message": "这个练习太难了" }
     *
     * 学生提交反馈，系统自动判断意图并走相应流水线：
     * - ACADEMIC_QUESTION → RAG 答疑（仅回答不调整）
     * - NEGATIVE_FEEDBACK/SUGGESTION → 反馈→策略→重推荐
     * - POSITIVE_FEEDBACK → 仅记录和回复
     */
    @PostMapping("/feedback")
    public Map<String, Object> feedback(@RequestBody Map<String, Object> body,
                                         HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        Long studentId = extractLong(body, "studentId");
        Long courseId = extractLong(body, "courseId");
        String message = extractString(body, "message");

        if (studentId == null || message == null || message.isBlank()) {
            return wrapError("参数缺失：studentId 和 message 为必填项");
        }

        try {
            Map<String, Object> result = riskCenterService.processFeedback(studentId, courseId, message);
            String mode = StrUtil.toString(result.get("mode"), "UNKNOWN");
            String reply = StrUtil.toString(result.get("reply"), "");
            result.put("message", "FEEDBACK_LOOP".equals(mode)
                    ? "反馈已处理，策略已自动调整"
                    : reply.isBlank() ? "反馈已记录" : reply);
            return wrapSuccess(result);
        } catch (Exception e) {
            log.error("[AgentAPI] 反馈处理失败，studentId={}", studentId, e);
            return wrapError("反馈处理失败: " + e.getMessage());
        }
    }

    // ===== 流水线 3：端到端闭环 =====

    /**
     * POST /api/agent/close-loop/{studentId}
     * Body: { "courseId": 2, "feedback": "我觉得学习节奏太快了" }
     *
     * 端到端闭环：完整评估 → 反馈分析 → 策略调整 → 重推荐
     * 一次请求走完监控→分析→画像→推荐→反馈→策略→重推荐全链路
     */
    @PostMapping("/close-loop/{studentId}")
    public Map<String, Object> closeLoop(@PathVariable Long studentId,
                                          @RequestBody(required = false) Map<String, Object> body,
                                          HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        Long courseId = body != null ? extractLong(body, "courseId") : null;
        String feedback = body != null ? extractString(body, "feedback") : null;

        try {
            Map<String, Object> result = riskCenterService.runCloseLoop(studentId, courseId, feedback);
            result.put("message", Boolean.TRUE.equals(result.get("closeLoopCompleted"))
                    ? "端到端闭环已完成"
                    : "评估已完成（未提供反馈，跳过了闭环阶段）");
            return wrapSuccess(result);
        } catch (Exception e) {
            log.error("[AgentAPI] 闭环流水线失败，studentId={}", studentId, e);
            return wrapError("闭环流水线失败: " + e.getMessage());
        }
    }

    // ===== 流水线 4：效果驱动调整 =====

    /**
     * POST /api/agent/effect-check/{studentId}
     * Body: { "courseId": 2 }
     *
     * 基于效果的自动策略调整：
     * 检查学生近期练习完成情况、得分变化、薄弱点演变
     * 无需人工反馈，自动决策是否调整学习策略
     */
    @PostMapping("/effect-check/{studentId}")
    public Map<String, Object> effectCheck(@PathVariable Long studentId,
                                            @RequestBody(required = false) Map<String, Object> body,
                                            HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        Long courseId = body != null ? extractLong(body, "courseId") : null;

        try {
            Map<String, Object> result = riskCenterService.runEffectCheck(studentId, courseId);
            result.put("message", Boolean.TRUE.equals(result.get("adjustmentTriggered"))
                    ? "策略已自动调整"
                    : "当前效果良好，无需调整");
            return wrapSuccess(result);
        } catch (Exception e) {
            log.error("[AgentAPI] 效果检查失败，studentId={}", studentId, e);
            return wrapError("效果检查失败: " + e.getMessage());
        }
    }

    // ===== RAG 知识库问答 =====

    /**
     * POST /api/agent/qa
     * Body: { "question": "什么是递归？", "courseId": 1(可选) }
     *
     * 基于百炼 RAG 知识库的学术问答
     * 支持按课程路由到对应知识库应用，实现课程级知识隔离检索
     */
    @PostMapping("/qa")
    public Map<String, Object> qa(@RequestBody Map<String, Object> body,
                                   HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        String question = extractString(body, "question");
        if (question == null || question.isBlank()) {
            return wrapError("参数缺失：question 为必填项");
        }

        Long courseId = null;
        if (body.containsKey("courseId") && body.get("courseId") != null) {
            try {
                courseId = Long.valueOf(body.get("courseId").toString());
            } catch (NumberFormatException ignored) {}
        }

        try {
            Map<String, Object> result = riskCenterService.answerQuestion(question, courseId);
            return wrapSuccess(result);
        } catch (Exception e) {
            log.error("[AgentAPI] RAG 问答失败", e);
            return wrapError("RAG 知识库检索失败: " + e.getMessage());
        }
    }

    // ===== 工具调用问答（W3） =====

    /**
     * POST /api/agent/ask
     * Body: { "studentId": 2, "courseId": 1, "question": "这个学生最近哪门课最危险？" }
     *
     * 工具调用问答：DataQueryAgent 自主决定调用哪些数据查询工具（成绩/预警/画像/趋势/记忆…），
     * 基于真实库中数据作答，并返回本次调用的工具轨迹（toolTrace）便于教师核对。
     */
    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        Long studentId = extractLong(body, "studentId");
        Long courseId = extractLong(body, "courseId");
        String question = extractString(body, "question");

        if (studentId == null || question == null || question.isBlank()) {
            return wrapError("参数缺失：studentId 和 question 为必填项");
        }

        try {
            Map<String, Object> result = riskCenterService.askWithTools(studentId, courseId, question);
            return wrapSuccess(result);
        } catch (Exception e) {
            log.error("[AgentAPI] 工具调用问答失败，studentId={}", studentId, e);
            return wrapError("工具调用问答失败: " + e.getMessage());
        }
    }

    // ===== 流水线统计 =====

    /**
     * GET /api/agent/stats
     *
     * 查看所有可用智能体和流水线信息
     */
    @GetMapping("/stats")
    public Map<String, Object> stats(HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();
        try {
            Map<String, Object> stats = riskCenterService.getPipelineStats();
            return wrapSuccess(stats);
        } catch (Exception e) {
            return wrapError("统计查询失败: " + e.getMessage());
        }
    }

    /**
     * GET /api/agent/runs?studentId=&limit=
     *
     * 智能体运行历史（教师端"运行历史"面板）：每次流水线一条记录，含各步智能体明细。
     * 不传 studentId 时返回全体最近运行。
     */
    @GetMapping("/runs")
    public Map<String, Object> runs(@RequestParam(required = false) Long studentId,
                                    @RequestParam(defaultValue = "10") int limit,
                                    HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("data", agentRunService.listRuns(studentId, limit));
            return result;
        } catch (Exception e) {
            log.error("[AgentAPI] 查询运行历史失败", e);
            return wrapError("查询运行历史失败: " + e.getMessage());
        }
    }

    /** GET /api/agent/runs/{runId} — 单次运行明细 */
    @GetMapping("/runs/{runId}")
    public Map<String, Object> runDetail(@PathVariable Long runId, HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("data", agentRunService.getRunDetail(runId));
            return result;
        } catch (Exception e) {
            log.error("[AgentAPI] 查询运行明细失败，runId={}", runId, e);
            return wrapError("查询运行明细失败: " + e.getMessage());
        }
    }

    /**
     * GET /api/agent/eval?live=false&amp;limit=20
     *
     * Golden set 评测（W2）：live=false 用已落库的运行结果评测（不消耗额度）；
     * live=true 会按 golden 用例逐个真实跑完整评估并判定（消耗大模型额度，用于论文出表）。
     */
    @GetMapping("/eval")
    public Map<String, Object> eval(@RequestParam(defaultValue = "false") boolean live,
                                    @RequestParam(defaultValue = "golden") String mode,
                                    @RequestParam(defaultValue = "20") int limit,
                                    HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("data", "ablation".equalsIgnoreCase(mode)
                    ? agentEvalService.ablation(limit, live)
                    : agentEvalService.evaluate(limit, live));
            return result;
        } catch (Exception e) {
            log.error("[AgentAPI] golden set 评测失败", e);
            return wrapError("评测失败: " + e.getMessage());
        }
    }

    // ===== 批量效果检查 =====

    /**
     * POST /api/agent/batch-effect-check
     * Body: { "studentIds": [1, 2, 3], "courseId": 2 }
     *
     * 批量检查多个学生的效果并自动调整策略
     * 适合定时任务或教师批量操作
     */
    @PostMapping("/batch-effect-check")
    public Map<String, Object> batchEffectCheck(@RequestBody Map<String, Object> body,
                                                  HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        java.util.List<?> rawIds = body.get("studentIds") instanceof java.util.List
                ? (java.util.List<?>) body.get("studentIds") : java.util.List.of();
        Long courseId = extractLong(body, "courseId");

        if (rawIds.isEmpty()) {
            return wrapError("参数缺失：studentIds 不能为空");
        }

        java.util.List<Map<String, Object>> results = new java.util.ArrayList<>();
        int adjusted = 0, unchanged = 0, failed = 0;

        for (Object idObj : rawIds) {
            Long sid = idObj instanceof Number ? ((Number) idObj).longValue() : null;
            if (sid == null) continue;
            try {
                Map<String, Object> r = riskCenterService.runEffectCheck(sid, courseId);
                if (Boolean.TRUE.equals(r.get("adjustmentTriggered"))) adjusted++;
                else if (Boolean.TRUE.equals(r.get("success"))) unchanged++;
                else failed++;
                results.add(Map.of(
                        "studentId", sid,
                        "adjustmentTriggered", r.getOrDefault("adjustmentTriggered", false),
                        "success", r.getOrDefault("success", false)));
            } catch (Exception e) {
                failed++;
                results.add(Map.of("studentId", sid, "success", false, "error", e.getMessage()));
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("success", true);
        summary.put("total", rawIds.size());
        summary.put("adjustedCount", adjusted);
        summary.put("unchangedCount", unchanged);
        summary.put("failedCount", failed);
        summary.put("details", results);
        summary.put("message", String.format("批量检查完成：%d人策略已调整，%d人无需调整，%d人失败",
                adjusted, unchanged, failed));
        return summary;
    }

    // ===== 学习计划反馈快捷入口 =====

    /**
     * POST /api/agent/plan-feedback
     * Body: { "studentId": 1, "planId": 100, "feedbackType": "TOO_HARD", "comment": "太难了" }
     *
     * 学生对推荐的学习计划提交反馈
     * feedbackType: TOO_HARD / TOO_EASY / IRRELEVANT / TOO_FAST / TOO_SLOW
     */
    @PostMapping("/plan-feedback")
    public Map<String, Object> planFeedback(@RequestBody Map<String, Object> body,
                                             HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        Long studentId = extractLong(body, "studentId");
        Long planId = extractLong(body, "planId");
        String feedbackType = extractString(body, "feedbackType");
        String comment = extractString(body, "comment");

        if (studentId == null || planId == null || feedbackType == null) {
            return wrapError("参数缺失：studentId、planId、feedbackType 为必填项");
        }

        try {
            Map<String, Object> result = riskCenterService.handlePlanFeedback(
                    studentId, planId, feedbackType, comment);
            result.put("message", Boolean.TRUE.equals(result.get("adjustmentTriggered"))
                    ? "反馈已处理，学习计划已调整"
                    : "感谢反馈！当前计划很合适，继续加油！");
            return wrapSuccess(result);
        } catch (Exception e) {
            log.error("[AgentAPI] 计划反馈失败", e);
            return wrapError("反馈处理失败: " + e.getMessage());
        }
    }

    // ===== 辅助方法 =====

    private boolean authCheck(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) return false;
        if (auth.startsWith("Bearer ")) auth = auth.substring(7).trim();
        return tokenService.validateAndGetTeacherId(auth) != null
                || tokenService.validateAndGetStudentId(auth) != null;
    }

    private Map<String, Object> unauthorizeResult() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("message", "请先登录");
        return r;
    }

    private Map<String, Object> wrapSuccess(Object data) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> d = (Map<String, Object>) data;
            r.putAll(d);
        } else {
            r.put("data", data);
        }
        return r;
    }

    private Map<String, Object> wrapError(String message) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("message", message);
        return r;
    }

    private Long extractLong(Map<String, Object> body, String key) {
        Object v = body != null ? body.get(key) : null;
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try { return Long.parseLong((String) v); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private String extractString(Map<String, Object> body, String key) {
        Object v = body != null ? body.get(key) : null;
        return v != null ? v.toString() : null;
    }

    /**
     * 便捷 toString 封装（避免与 java.util.Objects 混淆）
     */
    private static final class StrUtil {
        static String toString(Object obj, String defaultStr) {
            return obj != null ? obj.toString() : defaultStr;
        }
    }
}
