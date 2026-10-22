package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 一次判定的结果统计 */
@Data
public class RiskEvaluateResultVO {
    /** 本次判定的选课条数 */
    private int evaluatedCount;
    /** 新增的风险状态行数 */
    private int insertedCount;
    /** 覆盖更新的风险状态行数 */
    private int updatedCount;
    /** 等级发生变化（升/降）的条数，供第 2 步生成预警事件用 */
    private int levelChangedCount;
    /** 本次使用的引擎版本 */
    private String engineVersion;
    /** 判定触发来源：EVALUATION / DATA_CHANGE / MANUAL */
    private String trigger;
    /** 影响范围描述，如「教学班 CS-01-A」 */
    private String scope;
    /** 耗时（毫秒） */
    private long costMs;
    /** 判定时间 */
    private LocalDateTime evaluatedAt;

    public static RiskEvaluateResultVO empty(String scope) {
        RiskEvaluateResultVO v = new RiskEvaluateResultVO();
        v.setScope(scope);
        v.setEvaluatedAt(LocalDateTime.now());
        return v;
    }

    /** 便于计算的快捷返回（等级变化计数在 Service 内累加） */
    public RiskEvaluateResultVO plusLevelChanged(int n) {
        this.levelChangedCount += n;
        return this;
    }

    public BigDecimal getLevelChangedRatio() {
        if (evaluatedCount <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(levelChangedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(evaluatedCount), 1, java.math.RoundingMode.HALF_UP);
    }
}
