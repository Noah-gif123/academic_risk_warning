package com.example.academic_risk_warning.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 异步 embedding worker：每 8 秒消费一批任务，失败按 PENDING 重试，3 次后 FAILED。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KbEmbeddingWorker {

    private final KbEmbeddingService service;

    @Scheduled(fixedDelay = 8_000, initialDelay = 8_000)
    public void run() {
        try {
            var r = service.processJobs(20);
            if (((Number) r.get("picked")).intValue() > 0) {
                log.info("[KB-EMBED] {}", r);
            }
        } catch (Exception e) {
            // 误连 v2 库时 ai_kb_embedding_job 不存在，避免每 8 秒刷 warning
            log.debug("[KB-EMBED] 跳过本轮：{}", e.getMessage());
        }
    }
}
