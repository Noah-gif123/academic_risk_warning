package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 风险引擎配置表（版本化）
 * <p>对应表：risk_engine_config</p>
 */
@Data
@TableName("risk_engine_config")
public class RiskEngineConfig {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 引擎版本号，如 v2.0.0；规则/权重/公式变化即升版 */
    private String engineVersion;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 是否当前生效版本（全库仅一条为1） */
    private Integer isActive;

    /** 通道A（确定性风险）开关 */
    private Integer channelAEnabled;

    /** 通道B（班内相对）开关 */
    private Integer channelBEnabled;

    /** 通道C（跨班可比，依赖Δ）开关 */
    private Integer channelCEnabled;

    /** 个体信号权重：{"freshman":{"attendance":0.40,"homework":0.30,"score":0.25,"subjective":0.05},"normal":{"attendance":0.30,"homework":0.25,"score":0.30,"subjective":0.15}} */
    private String signalWeightsJson;

    /** 是否启用教师尺度 Δ 归一化 */
    private Integer deltaEnabled;

    /** 是否启用课程因子 c 归一化 */
    private Integer courseFactorEnabled;

    /** 风险分档：红 ≥ */
    private BigDecimal thresholdRed;

    /** 橙 60–75 */
    private BigDecimal thresholdOrange;

    /** 黄 40–60（绿 <40） */
    private BigDecimal thresholdYellow;

    /** 点名覆盖率阈值（1/3；低于此值：提醒补录+教务督办+置信度判低） */
    private BigDecimal coverageThreshold;

    /** 期内软提醒：连续 N 周零录入即提醒 */
    private Integer coverageSoftWeeks;

    /** 导员班集体提醒起始周（第1–4周不触发） */
    private Integer counselorWeekStart;

    /** 班集体提醒档位1：预警占比 ≥ */
    private BigDecimal counselorRateL1;

    /** 档位1：绝对人数 ≥ */
    private Integer counselorMinL1;

    /** 档位2（升级）：占比 ≥ */
    private BigDecimal counselorRateL2;

    /** 档位2：绝对人数 ≥ */
    private Integer counselorMinL2;

    /** 导员端纳入范围：风险分 ≥（≥40 黄橙红进名单；绿不出现） */
    private BigDecimal counselorRiskFloor;

    /** 多科目预警门限（≥3 门触发"重点关注该学生"） */
    private Integer multiCourseThreshold;

    /** 干预无效判定最长观察天数（或下次 risk_state 刷新，先到为准） */
    private Integer invalidCheckDays;

    /** 扣分系数：每缺课一次 */
    private BigDecimal deductAbsent;

    /** 扣分系数：每迟到一次 */
    private BigDecimal deductLate;

    /** 扣分系数：超免扣次数后每次请假 */
    private BigDecimal deductLeave;

    /** 请假免扣次数（每学期每门课） */
    private Integer leaveFreeTimes;

    /** 期末满分（"期末需考 x > 此值 → 实际不可达"的硬上限） */
    private BigDecimal finalFullScore;

    /** Δ 收缩常数 k：λ = n/(n+k) */
    private Integer deltaShrinkK;

    /** Δ 可用最小样本量（低于则 λ<0.33，视为不可用） */
    private Integer deltaMinSample;

    /** |Δ| 熔断阈值：超过则停用该 Δ 并推教务复核 */
    private BigDecimal deltaFuseThreshold;

    /** 置信度硬规则：覆盖率达标且锚点为A/A′时，W 不得低于此值 */
    private BigDecimal confidenceMinOk;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
