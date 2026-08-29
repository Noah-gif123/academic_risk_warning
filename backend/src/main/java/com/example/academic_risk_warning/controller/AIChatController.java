package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.service.AIChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 助手接口：转发用户消息到大模型并返回回复
 */
@RestController
@RequestMapping("/api/ai")
public class AIChatController {

    private final AIChatService aiChatService;

    public AIChatController(AIChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /**
     * POST /api/ai/chat
     * Body: { "messages": [{ "role": "user", "content": "..." }] }
     * 任何已登录用户（教师/学生/管理员）均可使用
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body,
                                    HttpServletRequest request) {
        // 简易鉴权：只要有 Authorization header 就算已登录
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) {
            return Map.of("success", false, "message", "请先登录");
        }
        try {
            String reply = aiChatService.chat(body);
            return Map.of("success", true, "data", Map.of("reply", reply));
        } catch (Exception e) {
            return Map.of("success", false, "message", "AI 服务异常: " + e.getMessage());
        }
    }
}
