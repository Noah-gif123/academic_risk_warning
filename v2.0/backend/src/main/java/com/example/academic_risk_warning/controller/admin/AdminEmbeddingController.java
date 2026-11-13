package com.example.academic_risk_warning.controller.admin;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.service.ai.KbEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 方案 A：课程分块 embedding 回填（仅教务）。 */
@RestController
@RequestMapping("/api/admin/ai/embeddings")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminEmbeddingController {

    private final KbEmbeddingService service;

    @PostMapping("/backfill")
    public Result<Map<String, Object>> backfill(@RequestParam(defaultValue = "false") boolean force) {
        return Result.ok(service.backfill(force));
    }

    @GetMapping("/jobs")
    public Result<Map<String, Object>> jobs() {
        return Result.ok(service.jobStats());
    }

    @PostMapping("/jobs/retry")
    public Result<Map<String, Object>> retry() {
        return Result.ok(Map.of("retried", service.retryFailed()));
    }
}
