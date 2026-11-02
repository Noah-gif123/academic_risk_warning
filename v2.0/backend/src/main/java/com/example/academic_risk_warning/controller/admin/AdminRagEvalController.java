package com.example.academic_risk_warning.controller.admin;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.service.ai.RagEvalService;
import com.example.academic_risk_warning.service.ai.RetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * RAG 检索评测入口（R4）。
 *
 * <p>只做检索、不做生成，所以<b>不消耗模型 token</b>，可以随时跑：
 * <pre>
 *   POST /api/admin/ai/rag-eval?mode=FULLTEXT&amp;k=4      单模式
 *   GET  /api/admin/ai/rag-eval/compare?k=4             全文 / 向量 / 混合 三行对比
 * </pre>
 * 每次运行都会写一行 {@code ai_rag_eval_run}，三行对比表的数据就来自这张表。</p>
 */
@RestController
@RequestMapping("/api/admin/ai/rag-eval")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminRagEvalController {

    private final RagEvalService evalService;

    @PostMapping
    public Result<Map<String, Object>> run(@RequestParam(defaultValue = "HYBRID") String mode,
                                           @RequestParam(defaultValue = "4") int k,
                                           @RequestParam(required = false) Double minLex,
                                           @RequestParam(required = false) Integer maxPerDoc,
                                           @RequestParam(required = false) String note) {
        return Result.ok(evalService.run(RetrievalService.parseMode(mode), k,
                minLex == null ? 0 : minLex, maxPerDoc == null ? 0 : maxPerDoc,
                note == null || note.isBlank() ? null : note));
    }

    @GetMapping("/compare")
    public Result<Map<String, Object>> compare(@RequestParam(defaultValue = "4") int k,
                                               @RequestParam(required = false) Double minLex,
                                               @RequestParam(required = false) Integer maxPerDoc,
                                               @RequestParam(required = false) String note) {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        for (RetrievalService.Mode m : RetrievalService.Mode.values()) {
            Map<String, Object> r = evalService.run(m, k,
                    minLex == null ? 0 : minLex, maxPerDoc == null ? 0 : maxPerDoc,
                    note == null || note.isBlank() ? null : note);
            r.remove("detail");
            out.put(m.name(), r);
        }
        return Result.ok(out);
    }
}
