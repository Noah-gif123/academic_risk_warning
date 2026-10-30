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

    // ---------------- M16：C 通道 / 课程因子系数（未传则沿用当前生效版本） ----------------
    /** 考试课难度系数（≥1，卷面难 → 风险上调；默认 1.10） */
    private BigDecimal courseFactorExam;
    /** 考查课难度系数（≤1，大作业为主 → 风险下调；默认 0.90） */
    private BigDecimal courseFactorAssess;
    /** 该科历史挂科率基准（%，默认 5.00） */
    private BigDecimal failRateBase;
    /** 历史挂科率每高 5 个百分点的乘数（默认 1.05） */
    private BigDecimal failRateStep;
    /** 历史挂科率系数上限（默认 1.20） */
    private BigDecimal failRateCap;
    /** 个人历史 1 门前科系数（默认 1.05） */
    private BigDecimal personalPrior1;
    /** 个人历史 ≥2 门前科系数（默认 1.10） */
    private BigDecimal personalPrior2;
    /** 严格型教师系数（默认 1.05） */
    private BigDecimal teacherStrictFactor;
    /** 宽松型教师系数（默认 0.95） */
    private BigDecimal teacherLooseFactor;

    /** 调整说明（会写进审计日志） */
    private String remark;
}
