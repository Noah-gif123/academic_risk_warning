package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.config.AiProperties;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.service.ai.AiRunService;
import com.example.academic_risk_warning.service.ai.DifyClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 通道探活与用量（M1 底座）。
 *
 * <p>{@code GET /api/ai/health} 不需要登录：部署体检脚本直接打它，就能知道
 * "Dify 起没起、报告工作流配没配、AI 开没开"。</p>
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiProperties props;
    private final DifyClient difyClient;
    private final AiRunService runService;

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", props.isEnabled());
        m.put("baseUrl", props.getBaseUrl());
        m.put("model", props.getModel());
        m.put("reportWorkflowReady", props.reportReady());
        m.put("questionWorkflowReady", props.getQuestionAppKey() != null && !props.getQuestionAppKey().isBlank());
        m.put("dailyLimit", props.getDailyLimit());
        m.put("reachable", props.isEnabled() && difyClient.reachable());
        m.put("usage", runService.summary());
        return Result.ok(m);
    }

    @GetMapping("/usage")
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> usage() {
        return Result.ok(runService.summary());
    }

    /** 手动触发一次探活（前端"测试连接"按钮用） */
    @PostMapping("/ping")
    public Result<Map<String, Object>> ping(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reachable", difyClient.reachable());
        m.put("baseUrl", props.getBaseUrl());
        return Result.ok(m);
    }
}
