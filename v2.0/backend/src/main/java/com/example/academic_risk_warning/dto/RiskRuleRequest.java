package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 判定规则配置请求（第二阶段第 5 步）。
 *
 * <p>新建版本时未传的字段沿用当前生效版本；阈值与系数口径见 risk_engine_config。</p>
 */
@Data
public class RiskRuleRequest {
    /** 版本号，留空则按当前版本自动 +0.1.0（如 v2.0.0 → v2.1.0） */
    private String engineVersion;
    /** 红色阈值（≥ 即红） */
    private BigDecimal thresholdRed;
    /** 橙色阈值（≥ 即橙） */
    private BigDecimal thresholdOrange;
    /** 黄色阈值（≥ 即黄） */
    private BigDecimal thresholdYellow;
    /** 每次缺课扣分 */
    private BigDecimal deductAbsent;
    /** 每次迟到扣分 */
    private BigDecimal deductLate;
    /** 超出免扣次数后每次请假扣分 */
    private BigDecimal deductLeave;
    /** 每学期每门课请假免扣次数 */
    private Integer leaveFreeTimes;
    /** 期末满分（期末需考超过此值即视为不可达） */
    private BigDecimal finalFullScore;
    /** 点名覆盖率阈值（低于则低置信度） */
    private BigDecimal coverageThreshold;
    /** 置信度下限（低于则只输出行为预警） */
    private BigDecimal confidenceMinOk;
    /** 调整说明（会写进审计日志） */
    private String remark;
}
