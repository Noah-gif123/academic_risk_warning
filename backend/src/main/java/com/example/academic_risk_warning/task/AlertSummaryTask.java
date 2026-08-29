package com.example.academic_risk_warning.task;

import com.example.academic_risk_warning.service.AlertSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 预警汇总定时任务
 */
@Component
public class AlertSummaryTask {

    private static final Logger log = LoggerFactory.getLogger(AlertSummaryTask.class);
    private final AlertSummaryService alertSummaryService;

    public AlertSummaryTask(AlertSummaryService alertSummaryService) {
        this.alertSummaryService = alertSummaryService;
    }

    /** 每天早上8点生成每日汇总 */
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDailySummary() {
        log.info("【汇总】开始生成每日预警汇总...");
        try {
            alertSummaryService.generateAllSummaries("DAILY");
            log.info("【汇总】每日预警汇总完成");
        } catch (Exception e) {
            log.error("【汇总】每日汇总失败: {}", e.getMessage(), e);
        }
    }

    /** 每周一早8点生成周报 */
    @Scheduled(cron = "0 0 8 ? * MON")
    public void sendWeeklySummary() {
        log.info("【汇总】开始生成周度预警报告...");
        try {
            alertSummaryService.generateAllSummaries("WEEKLY");
            log.info("【汇总】周度预警报告完成");
        } catch (Exception e) {
            log.error("【汇总】周度报告失败: {}", e.getMessage(), e);
        }
    }
}
