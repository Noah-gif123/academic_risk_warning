package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.agent.feedback.FeedbackAgent;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.service.TeacherContextService;
import com.example.academic_risk_warning.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 教师端智能体 API：提供基于真实数据的分析工具和推荐问题
 *
 * 与 AgentController（学生端/通用流水线）区分：
 * - 本 Controller 面向教师，注入教师上下文（teacherId + courseId）
 * - 工具调用直接查数据库获取真实数据，再结合 LLM 生成分析
 */
@RestController
@RequestMapping("/api/agent/teacher")
public class TeacherAgentController {

    private static final Logger log = LoggerFactory.getLogger(TeacherAgentController.class);

    private final TeacherContextService teacherContextService;
    private final FeedbackAgent feedbackAgent;
    private final TokenService tokenService;

    public TeacherAgentController(TeacherContextService teacherContextService,
                                   FeedbackAgent feedbackAgent,
                                   TokenService tokenService) {
        this.teacherContextService = teacherContextService;
        this.feedbackAgent = feedbackAgent;
        this.tokenService = tokenService;
    }

    // ===== 1. 数据驱动的推荐问题 =====

    /**
     * GET /api/agent/teacher/suggestions?teacherId=1&courseId=3
     *
     * 返回：根据当前课程实际数据动态生成的推荐问题列表
     * - 有预警数据 → 生成数据感知问题
     * - 无预警 → 生成规则/配置相关问题
     */
    @GetMapping("/suggestions")
    public Map<String, Object> getSuggestions(@RequestParam Long teacherId,
                                               @RequestParam(required = false) Long courseId,
                                               HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        try {
            List<String> suggestions;
            if (courseId != null) {
                suggestions = teacherContextService.getTeacherSuggestions(teacherId, courseId);
            } else {
                // 没有指定课程时，返回通用问题
                suggestions = List.of(
                        "如何设置课程的预警规则和阈值？",
                        "学业预警系统的红橙黄三级分别代表什么？",
                        "教师收到预警后应采取哪些干预措施？",
                        "如何查看学生的知识点掌握情况？",
                        "怎样给学生推荐针对性练习题？",
                        "预警等级是如何计算的？"
                );
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("suggestions", suggestions);
            result.put("courseId", courseId);
            return result;
        } catch (Exception e) {
            log.error("[TeacherAgent] 生成推荐问题失败", e);
            return wrapError("生成推荐问题失败: " + e.getMessage());
        }
    }

    // ===== 2. 课程风险概览 =====

    /**
     * GET /api/agent/teacher/risk-overview?teacherId=1&courseId=3
     *
     * 返回：课程风险分布 + Top N 风险学生 + AI 解读
     */
    @GetMapping("/risk-overview")
    public Map<String, Object> getRiskOverview(@RequestParam Long teacherId,
                                                @RequestParam Long courseId,
                                                HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        try {
            Map<String, Object> data = teacherContextService.getRiskOverview(teacherId, courseId);
            if (data.isEmpty()) {
                return wrapError("暂无该课程的预警数据");
            }

            // 拼接上下文，让 LLM 基于真实数据生成分析解读
            String analysis = feedbackAgent.answerQuestion(buildRiskAnalysisPrompt(data));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("courseName", data.get("courseName"));
            result.put("totalStudents", data.get("totalStudents"));
            result.put("alertDistribution", data.get("alertDistribution"));
            result.put("topRiskStudents", data.get("topRiskStudents"));
            result.put("aiAnalysis", analysis);
            return result;
        } catch (Exception e) {
            log.error("[TeacherAgent] 风险概览查询失败", e);
            return wrapError("风险概览查询失败: " + e.getMessage());
        }
    }

    // ===== 3. 知识点薄弱项 =====

    /**
     * GET /api/agent/teacher/weak-points?teacherId=1&courseId=3
     *
     * 返回：课程知识点薄弱项列表（按错误率降序）
     */
    @GetMapping("/weak-points")
    public Map<String, Object> getWeakPoints(@RequestParam Long teacherId,
                                              @RequestParam Long courseId,
                                              HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        try {
            List<Map<String, Object>> weakPoints = teacherContextService.getCourseWeakPoints(teacherId, courseId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("courseId", courseId);
            result.put("weakPoints", weakPoints);
            result.put("totalCount", weakPoints.size());
            return result;
        } catch (Exception e) {
            log.error("[TeacherAgent] 薄弱点查询失败", e);
            return wrapError("薄弱点查询失败: " + e.getMessage());
        }
    }

    // ===== 4. 预警规则说明 =====

    /**
     * GET /api/agent/teacher/alert-rules?courseId=3
     *
     * 返回：当前课程的预警规则配置详情
     */
    @GetMapping("/alert-rules")
    public Map<String, Object> getAlertRules(@RequestParam Long courseId,
                                              HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        try {
            List<Map<String, Object>> rules = teacherContextService.getAlertRules(courseId);

            // 生成可读的规则说明
            String explanation = buildRuleExplanation(rules);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("courseId", courseId);
            result.put("rules", rules);
            result.put("explanation", explanation);
            return result;
        } catch (Exception e) {
            log.error("[TeacherAgent] 规则查询失败", e);
            return wrapError("规则查询失败: " + e.getMessage());
        }
    }

    // ===== 5. 干预方案生成 =====

    /**
     * POST /api/agent/teacher/intervention-plan
     * Body: { "teacherId": 1, "courseId": 3 }
     *
     * 返回：基于真实课程数据 + LLM 分析的干预措施方案
     */
    @PostMapping("/intervention-plan")
    public Map<String, Object> getInterventionPlan(@RequestBody Map<String, Object> body,
                                                     HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        Long teacherId = extractLong(body, "teacherId");
        Long courseId = extractLong(body, "courseId");

        if (teacherId == null || courseId == null) {
            return wrapError("参数缺失：teacherId 和 courseId 为必填项");
        }

        try {
            Map<String, Object> interventionData = teacherContextService.getInterventionData(teacherId, courseId);

            // 调用 LLM 基于真实数据生成干预方案
            String prompt = buildInterventionPrompt(interventionData);
            String plan = feedbackAgent.answerQuestion(prompt);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("courseId", courseId);
            result.put("riskData", interventionData.get("riskOverview"));
            result.put("topWeakPoints", interventionData.get("topWeakPoints"));
            result.put("interventionPlan", plan);
            return result;
        } catch (Exception e) {
            log.error("[TeacherAgent] 干预方案生成失败", e);
            return wrapError("干预方案生成失败: " + e.getMessage());
        }
    }

    // ===== 6. 教学建议 =====

    /**
     * POST /api/agent/teacher/teaching-advice
     * Body: { "teacherId": 1, "courseId": 3 }
     *
     * 返回：基于薄弱知识点和风险概览的针对性教学建议
     */
    @PostMapping("/teaching-advice")
    public Map<String, Object> getTeachingAdvice(@RequestBody Map<String, Object> body,
                                                   HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        Long teacherId = extractLong(body, "teacherId");
        Long courseId = extractLong(body, "courseId");

        if (teacherId == null || courseId == null) {
            return wrapError("参数缺失：teacherId 和 courseId 为必填项");
        }

        try {
            List<Map<String, Object>> weakPoints = teacherContextService.getCourseWeakPoints(teacherId, courseId);

            // 调用 LLM 基于薄弱知识点生成教学建议
            String prompt = buildTeachingAdvicePrompt(weakPoints);
            String advice = feedbackAgent.answerQuestion(prompt);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("courseId", courseId);
            result.put("weakPointsCount", weakPoints.size());
            result.put("teachingAdvice", advice);
            return result;
        } catch (Exception e) {
            log.error("[TeacherAgent] 教学建议生成失败", e);
            return wrapError("教学建议生成失败: " + e.getMessage());
        }
    }

    // ===== 7. 教师课程列表 =====

    /**
     * GET /api/agent/teacher/courses?teacherId=1
     *
     * 返回：教师教授的所有课程
     */
    @GetMapping("/courses")
    public Map<String, Object> getTeacherCourses(@RequestParam Long teacherId,
                                                   HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        try {
            List<Course> courses = teacherContextService.getTeacherCourses(teacherId);
            List<Map<String, Object>> courseList = courses.stream().map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", c.getId());
                m.put("courseName", c.getCourseName());
                return m;
            }).collect(java.util.stream.Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("courses", courseList);
            return result;
        } catch (Exception e) {
            log.error("[TeacherAgent] 课程查询失败", e);
            return wrapError("课程查询失败: " + e.getMessage());
        }
    }

    // ===== 8. 通用 RAG 问答（保留） =====

    /**
     * POST /api/agent/teacher/qa
     * Body: { "question": "如何解读学生的风险等级？" }
     *
     * 基于百炼 RAG 知识库的通用问答（不绑定课程上下文）
     */
    @PostMapping("/qa")
    public Map<String, Object> teacherQA(@RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
        if (!authCheck(request)) return unauthorizeResult();

        String question = extractString(body, "question");
        if (question == null || question.isBlank()) {
            return wrapError("参数缺失：question 为必填项");
        }

        try {
            String reply = feedbackAgent.answerQuestion(question);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("reply", reply);
            return result;
        } catch (Exception e) {
            log.error("[TeacherAgent] RAG 问答失败", e);
            return wrapError("RAG 知识库检索失败: " + e.getMessage());
        }
    }

    // ===== 辅助方法 =====

    private String buildRiskAnalysisPrompt(Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        Map<String, Object> dist = (Map<String, Object>) data.get("alertDistribution");
        String courseName = Objects.toString(data.get("courseName"), "当前课程");
        int totalStudents = ((Number) data.get("totalStudents")).intValue();

        StringBuilder sb = new StringBuilder();
        sb.append("你是一位教育数据分析专家。请根据以下「").append(courseName).append("」的真实数据，给出一段200字以内的风险概览分析。\n\n");
        sb.append("学生总数：").append(totalStudents).append("人\n");
        sb.append("风险分布：\n");
        for (String level : List.of("RED", "ORANGE", "YELLOW", "GREEN")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> d = (Map<String, Object>) dist.get(level);
            if (d != null) {
                sb.append("  ").append(d.get("label")).append("：").append(d.get("count")).append("人\n");
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topRisk = (List<Map<String, Object>>) data.get("topRiskStudents");
        if (topRisk != null && !topRisk.isEmpty()) {
            sb.append("\n高风险学生示例：\n");
            for (int i = 0; i < Math.min(5, topRisk.size()); i++) {
                Map<String, Object> s = topRisk.get(i);
                sb.append("  ").append(s.get("studentName"))
                  .append("（等级：").append(s.get("alertLevel"))
                  .append("，总分：").append(s.get("riskScore"))
                  .append("，原因：").append(s.get("alertReason")).append("）\n");
            }
        }

        sb.append("\n请分析：1) 整体风险态势 2) 需要重点关注的问题 3) 给教师的简要建议。用中文回答。");
        return sb.toString();
    }

    private String buildInterventionPrompt(Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        Map<String, Object> overview = (Map<String, Object>) data.get("riskOverview");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> weakPoints = (List<Map<String, Object>>) data.get("topWeakPoints");

        StringBuilder sb = new StringBuilder();
        sb.append("你是一位学业干预专家。请根据以下数据生成一份结构化的干预措施方案（300字以内）。\n\n");

        if (overview != null) {
            sb.append("## 课程风险概况\n");
            sb.append("课程：").append(overview.get("courseName")).append("\n");
            @SuppressWarnings("unchecked")
            Map<String, Object> dist = (Map<String, Object>) overview.get("alertDistribution");
            if (dist != null) {
                sb.append("风险分布：\n");
                for (String level : List.of("RED", "ORANGE", "YELLOW")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> d = (Map<String, Object>) dist.get(level);
                    if (d != null) {
                        sb.append("  ").append(d.get("label")).append("：").append(d.get("count")).append("人\n");
                    }
                }
            }
        }

        if (weakPoints != null && !weakPoints.isEmpty()) {
            sb.append("\n## TOP薄弱知识点\n");
            for (int i = 0; i < Math.min(5, weakPoints.size()); i++) {
                Map<String, Object> wp = weakPoints.get(i);
                sb.append("  ").append(i + 1).append(". ").append(wp.get("knowledgePointName"))
                  .append("（薄弱学生 ").append(wp.get("weakStudentCount"))
                  .append(" 人，平均错误率 ").append(wp.get("avgErrorRate")).append("%）\n");
            }
        }

        sb.append("\n请生成干预方案，包含：1) 紧急措施（对红色预警学生的行动）2) 知识点补救策略 3) 短期教学调整建议。用中文回答。");
        return sb.toString();
    }

    private String buildTeachingAdvicePrompt(List<Map<String, Object>> weakPoints) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位教学指导专家。请根据以下班级薄弱知识点，给出具体的教学建议（200字以内）。\n\n");

        if (weakPoints == null || weakPoints.isEmpty()) {
            sb.append("该课程暂无明显薄弱知识点。请给出通用的教学质量提升建议。\n");
        } else {
            sb.append("薄弱知识点列表（按错误率降序）：\n");
            for (int i = 0; i < Math.min(5, weakPoints.size()); i++) {
                Map<String, Object> wp = weakPoints.get(i);
                sb.append("  ").append(i + 1).append(". ").append(wp.get("knowledgePointName"))
                  .append(" — 薄弱学生 ").append(wp.get("weakStudentCount"))
                  .append(" 人，平均错误率 ").append(wp.get("avgErrorRate")).append("%\n");
            }
        }

        sb.append("\n请给出：1) 最需要优先补救的知识点及教学方法 2) 课堂教学节奏调整建议 3) 课后辅导策略。用中文回答。");
        return sb.toString();
    }

    private String buildRuleExplanation(List<Map<String, Object>> rules) {
        if (rules == null || rules.isEmpty()) {
            return "系统暂未配置任何预警规则，请先在管理端设置。";
        }

        Map<String, Object> rule = rules.get(0);
        boolean isDefault = rule.get("courseId") == null;
        StringBuilder sb = new StringBuilder();
        sb.append(isDefault ? "预警规则说明（体系默认规则）：\n\n" : "当前课程预警规则说明：\n\n");
        sb.append("■ 风险总分 = 学业维度 × ").append(rule.get("weightAcademic"))
          .append(" + 作业维度 × ").append(rule.get("weightHomework"))
          .append(" + 知识维度 × ").append(rule.get("weightKnowledge"))
          .append(" + 出勤维度 × ").append(rule.get("weightAttendance"))
          .append(" + 历史维度 × ").append(rule.get("weightHistory")).append("\n\n");
        sb.append("■ 等级阈值：\n");
        sb.append("  红色预警 ≥ ").append(rule.get("thresholdRed")).append("分\n");
        sb.append("  橙色预警 ≥ ").append(rule.get("thresholdOrange")).append("分\n");
        sb.append("  黄色预警 ≥ ").append(rule.get("thresholdYellow")).append("分\n\n");
        sb.append("■ 关键子阈值：\n");
        sb.append("  作业提交率阈值：").append(rule.get("homeworkSubmitRateThreshold")).append("%\n");
        sb.append("  知识点正确率阈值：").append(rule.get("knowledgeCorrectRateThreshold")).append("%\n");
        sb.append("  出勤率阈值：").append(rule.get("attendanceRateThreshold")).append("%\n");
        sb.append("  成绩骤降阈值：").append(rule.get("scoreDropThreshold")).append("\n");
        sb.append("  预测及格线：").append(rule.get("predictedPassScore"));
        return sb.toString();
    }

    // ===== 认证与工具方法 =====

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
}
