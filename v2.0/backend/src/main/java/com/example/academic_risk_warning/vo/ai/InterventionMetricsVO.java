package com.example.academic_risk_warning.vo.ai;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 干预闭环度量（M10）。
 *
 * <p>回答一个问题：**预警到底有没有用**。四个指标：
 * 干预覆盖率、回访有效率（转绿率）、平均响应时长、复发率。</p>
 */
@Data
public class InterventionMetricsVO {

    private Long termId;
    private String termName;
    /** 统计范围说明：全校 / 某辅导员名下行政班 */
    private String scopeText;

    /** 预警总数（未作废） */
    private Integer totalAlerts;
    /** 有干预记录的预警数 */
    private Integer coveredAlerts;
    /** 干预覆盖率（%） */
    private BigDecimal coverageRate;

    /** 干预记录总数 */
    private Integer interventions;
    /** 已完成回访（EFFECTIVE + INEFFECTIVE） */
    private Integer followed;
    private Integer effectiveCount;
    private Integer ineffectiveCount;
    private Integer pendingCount;
    /** 回访有效率（转绿率，%） */
    private BigDecimal effectiveRate;

    /** 平均响应时长（小时）：预警创建 → 首次干预 */
    private BigDecimal avgResponseHours;

    /** 复发预警数（干预后又产生新预警） */
    private Integer recurrenceCount;
    private BigDecimal recurrenceRate;

    /** 按等级拆分 */
    private List<ByLevel> byLevel = new ArrayList<>();

    /** 自动结论（数字全部可复算） */
    private String conclusion;

    @Data
    public static class ByLevel {
        private String level;
        private String levelText;
        private Integer alerts;
        private Integer followed;
        private Integer effective;
        private BigDecimal effectiveRate;
    }
}
