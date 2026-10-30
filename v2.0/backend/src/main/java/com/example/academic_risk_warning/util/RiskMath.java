package com.example.academic_risk_warning.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

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

    /**
     * M16：把"过程分覆盖率"乘进置信度。
     *
     * <p>为什么单独一个方法：`confidence_min_ok` 只允许把"点名覆盖率不足"抬起来，不能把
     * "分项只录了 5% 权重"也一起抬成"看起来可信"。所以调用方先用本方法把覆盖率乘进去，
     * 再只对点名覆盖率那一路做 floor。</p>
     */
    public static BigDecimal confidenceWithScoreCoverage(double coverage, double completeness,
                                                         double sampleAdequacy, BigDecimal scoreCoverage) {
        BigDecimal base = confidence(coverage, completeness, sampleAdequacy);
        double sc = scoreCoverage == null ? 1.0 : clamp01(scoreCoverage.doubleValue());
        return base.multiply(BigDecimal.valueOf(sc)).setScale(3, RoundingMode.HALF_UP);
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

    // ============================================================ M16：过程分 / r_final / 绝对口径
    /**
     * 一个叶子评分项在总评里的绝对权重。
     *
     * <p>构成是两级的（如「平时成绩 40」下挂「随堂测试 35」），子项权重是父项内部的百分比，
     * 所以绝对权重 = 父.weight × 子.weight / 100；根项自己就是叶子时直接用它的 weight。</p>
     *
     * <p>{@code terminal}：是否挂在终结性分支（根项 category=FINAL）下；
     * {@code available}：该分项在本教学班是否真有数据来源（手工有录入行 / 自动有试卷或项目）。</p>
     */
    public static final class SchemeWeight {
        public final long itemId;
        public final BigDecimal absWeight;
        public final boolean terminal;
        public final boolean available;

        public SchemeWeight(long itemId, BigDecimal absWeight, boolean terminal, boolean available) {
            this.itemId = itemId;
            this.absWeight = absWeight == null ? BigDecimal.ZERO : absWeight;
            this.terminal = terminal;
            this.available = available;
        }
    }

    /** 过程分结果：{@code score} 为 0–100 加权平均（无可用分项时为 null）；{@code coverage} 为 0–1 */
    public static final class ProcessScore {
        public final BigDecimal score;
        public final BigDecimal coverage;
        public final BigDecimal recordedWeight;
        public final BigDecimal totalWeight;

        ProcessScore(BigDecimal score, BigDecimal coverage, BigDecimal recordedWeight, BigDecimal totalWeight) {
            this.score = score;
            this.coverage = coverage;
            this.recordedWeight = recordedWeight;
            this.totalWeight = totalWeight;
        }
    }

    /**
     * 过程分 = 非终结性分支中"有数据来源"的分项的加权平均（0–100，按已录项归一）。
     *
     * <p>单个学生缺某个有来源的分项（缺交/缺考）按 0 分计入分子，分母不变——与成绩矩阵
     * "缺考未交记 0"的语义一致；真正"没录"的分项（available=false）不参与分母，避免用一项
     * 课堂表现代表整学期（见 M16 交接说明 §2）。</p>
     */
    public static ProcessScore processScore(List<SchemeWeight> leaves, Map<Long, BigDecimal> pctOf) {
        return branchAverage(leaves, pctOf, false);
    }

    /**
     * 终结性分支的加权平均（0–100）：该分支**已经录了分**时用它判断"还差多少"。
     *
     * <p>典型场景：考查课的大作业（终结性 60%）早就批完了。这时候再说"还需得 47 分"是错的，
     * 应该按"需求 X 分、实际拿了 Y 分 → 还差 max(0, X−Y)"来算。无数据时返回 null，仍按"需考"处理。</p>
     */
    public static ProcessScore terminalScore(List<SchemeWeight> leaves, Map<Long, BigDecimal> pctOf) {
        return branchAverage(leaves, pctOf, true);
    }

    /** 分支加权平均：terminal=true 只算终结性分支，false 只算非终结性分支 */
    private static ProcessScore branchAverage(List<SchemeWeight> leaves, Map<Long, BigDecimal> pctOf,
                                              boolean terminal) {
        BigDecimal recorded = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal points = BigDecimal.ZERO;
        for (SchemeWeight w : leaves) {
            if (w.terminal != terminal) {
                continue;
            }
            total = total.add(w.absWeight);
            if (!w.available) {
                continue;
            }
            recorded = recorded.add(w.absWeight);
            BigDecimal pct = pctOf == null ? null : pctOf.get(w.itemId);
            if (pct != null) {
                points = points.add(pct.multiply(w.absWeight).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            }
        }
        BigDecimal score = recorded.signum() > 0
                ? points.multiply(BigDecimal.valueOf(100)).divide(recorded, 2, RoundingMode.HALF_UP)
                : null;
        BigDecimal coverage = total.signum() > 0
                ? recorded.divide(total, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new ProcessScore(score, coverage, recorded.setScale(2, RoundingMode.HALF_UP),
                total.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * 终结性分支占全部分支的权重比（0–1），如期末 60% → 0.60。
     *
     * <p>必须按**根分支**的绝对权重算：`FINAL_EXAM` 自己的 weight 是父项内部的 100%，
     * 拿它当 r_final 会得到 1.0（现有 finalNeed 的 bug），从而把过程分整段丢掉。</p>
     */
    public static BigDecimal finalRatio(List<SchemeWeight> leaves) {
        BigDecimal terminal = BigDecimal.ZERO;
        BigDecimal all = BigDecimal.ZERO;
        for (SchemeWeight w : leaves) {
            all = all.add(w.absWeight);
            if (w.terminal) {
                terminal = terminal.add(w.absWeight);
            }
        }
        if (all.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return terminal.divide(all, 4, RoundingMode.HALF_UP);
    }

    /** 已得总评分估计 = 过程分 × (1 − r_final)：把过程分按非终结权重占比摊平成百分制点值 */
    public static BigDecimal estimatedEarned(BigDecimal processScore, BigDecimal finalRatio) {
        if (processScore == null) {
            return null;
        }
        BigDecimal nonFinal = BigDecimal.ONE.subtract(nz(finalRatio));
        return processScore.multiply(nonFinal).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 绝对口径风险分（分段映射，以"需考 = 及格线"为折点）。
     *
     * <p>本套数据里过程分普遍 80+，需考落在 35–75 之间。如果按"需考 60→0"一条直线，分数型风险全被压到绿的底部；
     * 但反过来把起点一直往下压，又会把"期末随便考考就能过"的人也刷成黄。所以分两段：</p>
     *
     * <ul>
     *   <li><b>需考 ≤ 及格线</b>（期末只要及格就能过）：`起点 → 0`，`及格线 → 50`；</li>
     *   <li><b>需考 &gt; 及格线</b>（期末必须考出超及格的水平）：`及格线 → 50`，满分 → 100。</li>
     * </ul>
     *
     * <p>于是 25→0、45→20、55→40、60→50、65→56、68→60（进橙）、80→75（进红）、100→100。
     * 起点 = 及格线 − {@code NEED_FLOOR_OFFSET}（35）是标定旋钮：调大更松、调小更严。</p>
     */
    public static final BigDecimal NEED_FLOOR_OFFSET = BigDecimal.valueOf(25);

    public static BigDecimal absoluteRiskScore(BigDecimal need, BigDecimal passScore, BigDecimal finalFullScore) {
        if (need == null) {
            return null;
        }
        BigDecimal full = nz(finalFullScore);
        BigDecimal pass = nz(passScore);
        BigDecimal floor = pass.subtract(NEED_FLOOR_OFFSET);
        BigDecimal raw;
        if (need.compareTo(pass) <= 0) {
            BigDecimal span = pass.subtract(floor);
            raw = span.signum() <= 0
                    ? BigDecimal.valueOf(50)
                    : need.subtract(floor).multiply(BigDecimal.valueOf(50)).divide(span, 2, RoundingMode.HALF_UP);
        } else {
            BigDecimal span = full.subtract(pass);
            raw = span.signum() <= 0
                    ? BigDecimal.valueOf(100)
                    : BigDecimal.valueOf(50).add(need.subtract(pass)
                    .multiply(BigDecimal.valueOf(50)).divide(span, 2, RoundingMode.HALF_UP));
        }
        return clamp100(raw.setScale(2, RoundingMode.HALF_UP));
    }

    // ============================================================ M16：C 通道因子的纯函数
    /**
     * 教师风格（连续版）：Δ 是"教师尺度"（同水平学生在这位老师手里的平均偏差，负值=偏严）。
     *
     * <p>旧版是三档离散（≤−1 严 / ≥+0.5 松 / 其间 1.00），导致 −0.46、−0.82 这种"偏严但不极端"的老师
     * 完全不加权。现在按指数连续映射：偏严侧用 {@code strictStep^(−Δ)}，偏松侧用 {@code looseStep^Δ}，
     * 结果夹在 [0.90, 1.15]；|Δ| 超过熔断阈值直接不用（数据可疑）；样本 &lt; 30 向 1.00 折半收缩。</p>
     */
    public static BigDecimal teacherStyleFactor(BigDecimal delta, Integer sampleSize, BigDecimal fuseThreshold,
                                                BigDecimal strictStep, BigDecimal looseStep) {
        if (delta == null) {
            return BigDecimal.ONE;
        }
        if (fuseThreshold != null && delta.abs().compareTo(fuseThreshold) > 0) {
            return BigDecimal.ONE;
        }
        BigDecimal strict = strictStep == null ? new BigDecimal("1.05") : strictStep;
        BigDecimal loose = looseStep == null ? new BigDecimal("0.95") : looseStep;
        double step = delta.signum() < 0 ? strict.doubleValue() : loose.doubleValue();
        double exponent = Math.abs(delta.doubleValue());
        BigDecimal factor = BigDecimal.valueOf(Math.pow(step, exponent)).setScale(4, RoundingMode.HALF_UP);
        if (factor.compareTo(new BigDecimal("0.90")) < 0) {
            factor = new BigDecimal("0.90");
        }
        if (factor.compareTo(new BigDecimal("1.15")) > 0) {
            factor = new BigDecimal("1.15");
        }
        if (sampleSize != null && sampleSize < 30) {
            factor = BigDecimal.ONE.add(factor.subtract(BigDecimal.ONE).multiply(new BigDecimal("0.5")))
                    .setScale(4, RoundingMode.HALF_UP);
        }
        return factor;
    }

    /**
     * 该科历史挂科率因子：以"每高 5 个百分点乘一次 step"的线性近似，夹在 [0.90, cap]。
     *
     * @param avgFailRate 历史平均挂科率（%，如 26.70）
     * @param examinees   历史样本人数；&lt; 30 时向 1.00 折半收缩（小样本不可信）
     */
    public static BigDecimal failRateFactor(BigDecimal avgFailRate, int examinees, BigDecimal base,
                                            BigDecimal step, BigDecimal cap) {
        if (avgFailRate == null) {
            return BigDecimal.ONE;
        }
        BigDecimal b = base == null ? new BigDecimal("5.00") : base;
        BigDecimal s = step == null ? new BigDecimal("1.05") : step;
        BigDecimal c = cap == null ? new BigDecimal("1.20") : cap;
        BigDecimal dev = avgFailRate.subtract(b).divide(new BigDecimal("5"), 4, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.add(dev.multiply(s.subtract(BigDecimal.ONE)));
        if (factor.compareTo(new BigDecimal("0.90")) < 0) {
            factor = new BigDecimal("0.90");
        }
        if (factor.compareTo(c) > 0) {
            factor = c;
        }
        if (examinees > 0 && examinees < 30) {
            factor = BigDecimal.ONE.add(factor.subtract(BigDecimal.ONE).multiply(new BigDecimal("0.5")));
        }
        return factor.setScale(4, RoundingMode.HALF_UP);
    }

    /** 一次挂科的权重：同一门课重修 ×2；再按时间衰减 1/(1+0.5×相隔学期数) */
    public static BigDecimal priorWeight(boolean sameCourse, int termsAgo) {
        BigDecimal w = sameCourse ? BigDecimal.valueOf(2) : BigDecimal.ONE;
        int ago = Math.max(0, termsAgo);
        return w.divide(BigDecimal.ONE.add(BigDecimal.valueOf(0.5).multiply(BigDecimal.valueOf(ago))),
                4, RoundingMode.HALF_UP);
    }

    /**
     * 个人前科因子：把每次挂科的权重加起来，&lt;0.5 视为无前科（1.00）、&lt;1.0 用 prior1、否则 prior2。
     *
     * <p>为什么按权重而不是"门数"：同一门课重修（尤其本课程重修班）比"挂过别的课"信号强得多，
     * 而三年前的挂科也不该和上学期的一样重。阈值取 1.0，正好让"同一门课挂一次"或"两门课各挂一次"
     * 进到 prior2。</p>
     */
    public static BigDecimal personalPriorFactor(BigDecimal weightedFails, BigDecimal prior1, BigDecimal prior2) {
        if (weightedFails == null || weightedFails.compareTo(new BigDecimal("0.5")) < 0) {
            return BigDecimal.ONE;
        }
        BigDecimal p1 = prior1 == null ? new BigDecimal("1.05") : prior1;
        BigDecimal p2 = prior2 == null ? new BigDecimal("1.10") : prior2;
        BigDecimal factor = weightedFails.compareTo(BigDecimal.ONE) < 0 ? p1 : p2;
        return factor.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 叠加 A 通道硬门槛与"再缺即挂"后的风险分：各项取最大，不连乘。
     *
     * <ul>
     *   <li>缺课超限（硬门槛）→ 95；</li>
     *   <li>期末/大作业不可达（需考 &gt; 满分）→ 100；</li>
     *   <li>剩余可缺 ≤1（再缺一次即挂）→ 至少 55；剩余 = 2 → 至少 40。</li>
     * </ul>
     */
    public static BigDecimal guardedRiskScore(BigDecimal need, BigDecimal passScore, BigDecimal finalFullScore,
                                              boolean hardFail, Integer remainingAbsent) {
        boolean unreachable = need != null && finalFullScore != null && need.compareTo(finalFullScore) > 0;
        BigDecimal base = absoluteRiskScore(need, passScore, finalFullScore);
        return clamp100(nz(base).max(guardFloor(hardFail, unreachable, remainingAbsent)));
    }

    /**
     * 确定性门槛的"地板分"（与课程/教师/个人乘子无关，不参与连乘）。
     *
     * <p>C 通道乘子只校准分数型风险；缺课硬线与"再缺即挂"是确定性判定，不能被 1.10 之类的系数
     * 抬起档位（否则 95×1.15 顶到 100，硬线基准分就解释不清了）。</p>
     */
    public static BigDecimal guardFloor(boolean hardFail, boolean unreachable, Integer remainingAbsent) {
        if (hardFail) {
            return BigDecimal.valueOf(95);
        }
        if (unreachable) {
            return BigDecimal.valueOf(100);
        }
        if (remainingAbsent != null) {
            if (remainingAbsent <= 1) {
                return BigDecimal.valueOf(55);
            }
            if (remainingAbsent == 2) {
                return BigDecimal.valueOf(40);
            }
        }
        return BigDecimal.ZERO;
    }


    /** 截断到 [0, 100] */
    public static BigDecimal clamp100(BigDecimal v) {
        if (v == null) {
            return null;
        }
        if (v.signum() < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (v.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        }
        return v;
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
