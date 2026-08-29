package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.llm.LLMClient;
import com.example.academic_risk_warning.service.StudentContextService;
import com.example.academic_risk_warning.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生端 AI 助手控制器
 * 提供基于学生个人学情数据的智能对话
 */
@RestController
@RequestMapping("/api/agent/student")
public class StudentAgentController {

    private static final Logger log = LoggerFactory.getLogger(StudentAgentController.class);

    private final StudentContextService studentContextService;
    private final TokenService tokenService;
    private final LLMClient llmClient;

    public StudentAgentController(StudentContextService studentContextService,
                                   TokenService tokenService,
                                   LLMClient llmClient) {
        this.studentContextService = studentContextService;
        this.tokenService = tokenService;
        this.llmClient = llmClient;
    }

    /**
     * 学生端智能对话
     * POST /api/agent/student/chat
     * Body: { question, courseId(可选) }
     * Header: Authorization: Bearer {token}
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();

        Long studentId = resolveStudentId(request);
        if (studentId == null) {
            result.put("success", false);
            result.put("reply", "请先登录后再使用AI助手。");
            return result;
        }

        String question = (String) body.getOrDefault("question", "");
        if (question == null || question.isBlank()) {
            result.put("success", false);
            result.put("reply", "请输入你的问题。");
            return result;
        }

        // 课程ID可选
        Long courseId = null;
        if (body.containsKey("courseId") && body.get("courseId") != null) {
            try {
                courseId = Long.valueOf(body.get("courseId").toString());
            } catch (NumberFormatException ignored) {}
        }

        try {
            // 构建学情上下文
            String context = studentContextService.buildStudentContext(studentId, courseId);

            // 构建系统提示词
            String systemPrompt = buildSystemPrompt(context);

            // 调用 LLM（传入 courseId 以路由到对应课程知识库）
            String rawReply = llmClient.ragQuery(question, courseId);

            result.put("success", true);
            result.put("reply", rawReply);
            result.put("contextUsed", context);
            result.put("courseId", courseId);
        } catch (Exception e) {
            log.error("学生AI助手对话失败: studentId={}, question={}", studentId, question, e);
            result.put("success", false);
            result.put("reply", "抱歉，AI助手暂时无法响应，请稍后再试。\n错误信息：" + e.getMessage());
        }

        return result;
    }

    /**
     * 获取学生课程列表（用于课程选择器）
     */
    @GetMapping("/courses")
    public Map<String, Object> courses(HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();

        Long studentId = resolveStudentId(request);
        if (studentId == null) {
            result.put("success", false);
            result.put("message", "未登录");
            return result;
        }

        try {
            List<Map<String, Object>> courses = studentContextService.getStudentCourses(studentId);
            result.put("success", true);
            result.put("courses", courses);
        } catch (Exception e) {
            log.error("获取学生课程列表失败: studentId={}", studentId, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * 获取动态推荐问题
     */
    @GetMapping("/suggestions")
    public Map<String, Object> suggestions(@RequestParam(required = false) Long courseId,
                                            HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();

        Long studentId = resolveStudentId(request);
        if (studentId == null) {
            result.put("success", false);
            result.put("suggestions", List.of("请先登录"));
            return result;
        }

        try {
            List<String> suggestions = studentContextService.getStudentSuggestions(studentId, courseId);
            result.put("success", true);
            result.put("suggestions", suggestions);
        } catch (Exception e) {
            log.error("获取学生推荐问题失败: studentId={}", studentId, e);
            result.put("success", false);
            result.put("suggestions", List.of("我目前的学习状态如何？", "该怎么提高学习效果？"));
        }

        return result;
    }

    /**
     * 获取学生个人风险诊断（结构化数据）
     */
    @GetMapping("/risk-profile")
    public Map<String, Object> riskProfile(@RequestParam(required = false) Long courseId,
                                            HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();

        Long studentId = resolveStudentId(request);
        if (studentId == null) {
            result.put("success", false);
            result.put("message", "未登录");
            return result;
        }

        try {
            Map<String, Object> profile = studentContextService.getStudentRiskProfile(studentId, courseId);
            result.put("success", true);
            result.put("data", profile);
        } catch (Exception e) {
            log.error("获取学生风险诊断失败: studentId={}", studentId, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    private String buildSystemPrompt(String context) {
        return """
            你是一位贴心的学业导师，正在为一所高校的「学业风险预警系统」服务。
            你的对话对象是学生本人，你要像知心学长/学姐一样，用温暖、鼓励的语气帮助学生分析问题、给出建议。
            
            === 学生当前学情数据 ===
            %s
            
            === 请遵守以下规则 ===
            1. 从学生的角度出发，用「你」来称呼学生。
            2. 如果学生有预警，先解释原因（用通俗易懂的语言），再给具体可行的改进建议。
            3. 如果有薄弱知识点，推荐具体的学习方法和复习策略。
            4. 建议要具体可执行，例如「每天花30分钟复习第X章」、「建议先完成课后习题第X题」。
            5. 语气温暖积极，先肯定优点再指出问题，避免让学生感到沮丧。
            6. 如果学生状态良好，也要真诚地表扬和鼓励。
            7. 回答控制在300字以内，重点突出。
            8. 不要透露你是一个AI模型。
            """.formatted(context);
    }

    private Long resolveStudentId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) return null;
        if (auth.startsWith("Bearer ")) auth = auth.substring(7).trim();
        return tokenService.validateAndGetTeacherId(auth);
    }
}
