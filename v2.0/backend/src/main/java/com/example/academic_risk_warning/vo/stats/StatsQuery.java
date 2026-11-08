package com.example.academic_risk_warning.vo.stats;

import lombok.Data;

/**
 * /api/stats 通用查询参数（P1-4）。
 *
 * <p>termId 为空取当前学期；period 支持 week/month/term；
 * 传了 startDate+endDate 时以自定义区间为准（格式 yyyy-MM-dd）。</p>
 */
@Data
public class StatsQuery {
    private Long termId;
    private String period = "week";
    private String startDate;
    private String endDate;
}
