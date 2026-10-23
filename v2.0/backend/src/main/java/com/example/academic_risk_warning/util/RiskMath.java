package com.example.academic_risk_warning.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 风险判定的纯计算口径（不依赖数据库，便于单元测试）。
 *
 * <p>与《第二阶段规划书》第 4 章、v2.0/sql/README 的冻结口径保持一致：</p>
 * <ul>
 *   <li>允许缺课 N_max = floor(有效课次 / 3)；</li>
 *   <li>剩余可缺 = N_max − 已缺；0 = 再缺一次即挂，≤ −1 = 必然挂科；</li>
 *   <li>扣分 P = 2×缺课 + 1×迟到 + 1×max(0, 请假−3)；</li>
 *   <li>期末需考 x = (及格线 + P − 其余项加权) / 期末权重占比；x &gt; 满分 → 不可达；</li>
 *   <li>班内相对风险分 = clamp(0, 100, (100 − 总评) + 3×缺课)；</li>
 *   <li>等级：红 ≥ 阈值红｜橙 ≥ 阈值橙｜黄 ≥ 阈值黄｜其余绿。</li>
 * </ul>
 */
public final class RiskMath {

    private RiskMath() {
    }

    /** 允许缺课上限：floor(有效课次 / 3)，至少 1 */
    public static int allowedAbsent(int effectiveSessions) {
        if (effectiveSessions <= 0) {
            return 0;
        }
        return Math.max(1, effectiveSessions / 3);
    }

    /** 剩余可缺次数（可为 0 或负数） */
    public static int remainingAbsent(int allowedAbsent, int absentCount) {
        return allowedAbsent - absentCount;
    }

    /** A 通道等级：FAIL 必然挂科 / DANGER 再缺一次即挂（剩余 ≤1）/ WATCH 警戒（剩余 2）/ SAFE 正常 */
    public static String channelALevel(int absentCount, int allowedAbsent) {
        if (absentCount > allowedAbsent) {
            return "FAIL";
        }
        int remaining = remainingAbsent(allowedAbsent, absentCount);
        if (remaining <= 1) {
            return "DANGER";
        }
        if (remaining <= 2) {
            return "WATCH";
        }
        return "SAFE";
    }

    /** 扣分 P = 2×缺课 + 1×迟到 + 1×max(0, 请假−3)；不设上限、不截断 */
    public static BigDecimal deductionPoints(int absent, int late, int leave, int leaveFreeTimes) {
        int overLeave = Math.max(0, leave - Math.max(0, leaveFreeTimes));
        int p = absent * 2 + late + overLeave;
        return BigDecimal.valueOf(p).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 期末需考分：x = (及格线 + P − 其余项加权) / 期末权重占比。
     *
     * @param passScore       及格线
     * @param deduction       扣分 P
     * @param weightedTotal   当前加权总分 W（不含扣分）
     * @param finalWeighted   期末项已获得的加权分（期末得分 × 权重占比），可为 0
     * @param finalRatio      期末项权重占比（0~1），如 35% → 0.35
     * @return [0] = 需考分（可能 > 满分），[1] = 是否可达（需考分 ≤ 期末满分）
     */
    public static BigDecimal[] finalExamNeeded(BigDecimal passScore, BigDecimal deduction,
                                               BigDecimal weightedTotal, BigDecimal finalWeighted,
                                               BigDecimal finalRatio, BigDecimal finalFullScore) {
        if (finalRatio == null || finalRatio.signum() <= 0) {
            return new BigDecimal[] { null, BigDecimal.ZERO };
        }
        BigDecimal rest = nz(weightedTotal).subtract(nz(finalWeighted));
        BigDecimal numerator = nz(passScore).add(nz(deduction)).subtract(rest);
        BigDecimal needed = numerator.divide(finalRatio, 1, RoundingMode.HALF_UP);
        // 平时分已过线时算出来会是负数，对外统一按 0 分展示（期末考 0 分也能及格）
        if (needed.signum() < 0) {
            needed = BigDecimal.ZERO;
        }
        boolean reachable = finalFullScore == null || needed.compareTo(finalFullScore) <= 0;
        return new BigDecimal[] { needed, reachable ? BigDecimal.ZERO : BigDecimal.ONE };
    }

    /** 班内相对风险分：clamp(0, 100, (100 − 总评) + 3×缺课) */
    public static BigDecimal channelBScore(BigDecimal totalScore, int absent) {
        BigDecimal base = BigDecimal.valueOf(100).subtract(nz(totalScore));
        BigDecimal score = base.add(BigDecimal.valueOf(absent * 3L));
        if (score.signum() < 0) {
            score = BigDecimal.ZERO;
        }
        if (score.compareTo(BigDecimal.valueOf(100)) > 0) {
            score = BigDecimal.valueOf(100);
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    /** 置信度 = 覆盖率 × 数据完备度 × 样本充足度，clamp 到 [0, 1] */
    public static BigDecimal confidence(double coverage, double completeness, double sampleAdequacy) {
        double c = clamp01(coverage) * clamp01(completeness) * clamp01(sampleAdequacy);
        return BigDecimal.valueOf(c).setScale(3, RoundingMode.HALF_UP);
    }

    /** 置信度档位 */
    public static String confidenceLevel(BigDecimal confidence) {
        double c = confidence == null ? 0 : confidence.doubleValue();
        if (c >= 0.7) {
            return "HIGH";
        }
        if (c >= 0.4) {
            return "MEDIUM";
        }
        if (c >= 0.15) {
            return "LOW";
        }
        return "VERY_LOW";
    }

    /**
     * 输出形态：覆盖率低于 1/3 时一律只给行为预警（数据不足，不给分数）。
     *
     * @param confidence       置信度
     * @param coverageRate     点名覆盖率（已点名课次 / 应完成课次），可为 null
     */
    public static String outputMode(BigDecimal confidence, Double coverageRate) {
        if (coverageRate != null && coverageRate < (1.0 / 3.0)) {
            return "BEHAVIOR_ONLY";
        }
        double c = confidence == null ? 0 : confidence.doubleValue();
        if (c >= 0.7) {
            return "FULL_SCORE";
        }
        if (c >= 0.4) {
            return "SCORE_WITH_NOTE";
        }
        if (c >= 0.15) {
            return "BEHAVIOR_ONLY";
        }
        return "INSUFFICIENT";
    }

    /** 等级：按引擎配置的红/橙/黄阈值分档 */
    public static String riskLevel(BigDecimal score, BigDecimal red, BigDecimal orange, BigDecimal yellow) {
        if (score == null) {
            return "GREEN";
        }
        if (red != null && score.compareTo(red) >= 0) {
            return "RED";
        }
        if (orange != null && score.compareTo(orange) >= 0) {
            return "ORANGE";
        }
        if (yellow != null && score.compareTo(yellow) >= 0) {
            return "YELLOW";
        }
        return "GREEN";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || v < 0) {
            return 0;
        }
        return Math.min(1, v);
    }
}
