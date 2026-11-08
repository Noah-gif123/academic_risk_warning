package com.example.academic_risk_warning.vo.stats;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 统计接口统一响应体（P1-4）。
 *
 * <p>所有 /api/stats 接口返回 Result&lt;StatsEnvelope&lt;T&gt;&gt;，data 层统一为：
 * current（本期）/ previous（上期，可为 null）/ deltaRate（环比，可为 null）/
 * dataStatus（OK / PARTIAL / NO_DATA）/ reason（缺数据原因）/ sampleSize / asOf / periodStart / periodEnd。</p>
 *
 * <p>P1-4 阶段物化表（P1-2/P1-3）尚未落地，历史与干预类指标只能返回 PARTIAL/NO_DATA，
 * 用 dataStatus + reason 显式表达，禁止用 0 冒充真实值。</p>
 */
@Data
public class StatsEnvelope<T> {

    public static final String OK = "OK";
    public static final String PARTIAL = "PARTIAL";
    public static final String NO_DATA = "NO_DATA";

    private T current;
    private T previous;
    private BigDecimal deltaRate;
    private String dataStatus;
    private String reason;
    private Integer sampleSize;
    private String asOf;
    private String periodStart;
    private String periodEnd;

    public static <T> StatsEnvelope<T> ok(T current) {
        StatsEnvelope<T> e = new StatsEnvelope<>();
        e.current = current;
        e.dataStatus = OK;
        return e;
    }

    public static <T> StatsEnvelope<T> partial(T current, String reason) {
        StatsEnvelope<T> e = new StatsEnvelope<>();
        e.current = current;
        e.dataStatus = PARTIAL;
        e.reason = reason;
        return e;
    }

    public static <T> StatsEnvelope<T> noData(String reason) {
        StatsEnvelope<T> e = new StatsEnvelope<>();
        e.dataStatus = NO_DATA;
        e.reason = reason;
        return e;
    }
}
