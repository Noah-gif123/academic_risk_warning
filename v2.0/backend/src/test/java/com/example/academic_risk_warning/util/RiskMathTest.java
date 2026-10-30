package com.example.academic_risk_warning.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 判定口径的边界测试（第二阶段第 1 步验收项 ②④）：
 * 硬门槛、剩余可缺 0 与 −1、期末不可达、覆盖率过低降级。
 */
class RiskMathTest {

    @Test
    @DisplayName("允许缺课上限 = floor(有效课次/3)，至少 1")
    void allowedAbsent() {
        assertEquals(5, RiskMath.allowedAbsent(16));
        assertEquals(4, RiskMath.allowedAbsent(12));
        assertEquals(1, RiskMath.allowedAbsent(2));
        assertEquals(0, RiskMath.allowedAbsent(0));
    }

    @Test
    @DisplayName("剩余可缺：0 = 再缺一次即挂，-1 = 必然挂科")
    void remainingAndLevel() {
        int allowed = RiskMath.allowedAbsent(16); // 5
        assertEquals(5, RiskMath.remainingAbsent(allowed, 0));
        assertEquals(1, RiskMath.remainingAbsent(allowed, 4));
        assertEquals(0, RiskMath.remainingAbsent(allowed, 5));
        assertEquals(-1, RiskMath.remainingAbsent(allowed, 6));

        assertEquals("SAFE", RiskMath.channelALevel(0, allowed));
        assertEquals("WATCH", RiskMath.channelALevel(3, allowed));   // 剩余 2
        assertEquals("DANGER", RiskMath.channelALevel(4, allowed));  // 剩余 1
        assertEquals("DANGER", RiskMath.channelALevel(5, allowed));  // 剩余 0
        assertEquals("FAIL", RiskMath.channelALevel(6, allowed));    // 超过上限
    }

    @Test
    @DisplayName("扣分 P = 2×缺课 + 1×迟到 + 1×max(0, 请假−3)，不截断")
    void deduction() {
        assertEquals(0, RiskMath.deductionPoints(0, 0, 0, 3).compareTo(BigDecimal.ZERO));
        assertEquals(0, RiskMath.deductionPoints(0, 0, 3, 3).compareTo(BigDecimal.ZERO));
        assertEquals(0, RiskMath.deductionPoints(2, 1, 4, 3).compareTo(BigDecimal.valueOf(6)));
        // 20 次缺课也不截断
        assertEquals(0, RiskMath.deductionPoints(20, 0, 0, 3).compareTo(BigDecimal.valueOf(40)));
    }

    @Test
    @DisplayName("期末需考：可达与不可达两种")
    void finalExamNeeded() {
        // 及格线 60、无扣分、当前加权 70、期末占 35% → 需要 -28.6 分（已达标）
        BigDecimal[] r1 = RiskMath.finalExamNeeded(BigDecimal.valueOf(60), BigDecimal.ZERO,
                BigDecimal.valueOf(70), BigDecimal.ZERO, new BigDecimal("0.35"), BigDecimal.valueOf(100));
        assertNotNull(r1[0]);
        assertEquals(0, r1[1].signum());

        // 及格线 60、扣分 20、当前加权 30、期末占 35% → 需 142.9 分 → 不可达
        BigDecimal[] r2 = RiskMath.finalExamNeeded(BigDecimal.valueOf(60), BigDecimal.valueOf(20),
                BigDecimal.valueOf(30), BigDecimal.ZERO, new BigDecimal("0.35"), BigDecimal.valueOf(100));
        assertEquals(0, r2[0].compareTo(new BigDecimal("142.9")));
        assertEquals(1, r2[1].signum());

        // 没有期末评分项时不给数值
        BigDecimal[] r3 = RiskMath.finalExamNeeded(BigDecimal.valueOf(60), BigDecimal.ZERO,
                BigDecimal.valueOf(70), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(100));
        assertNull(r3[0]);
    }

    @Test
    @DisplayName("班内相对风险分：clamp 到 0~100")
    void channelB() {
        assertEquals(0, RiskMath.channelBScore(BigDecimal.valueOf(70), 0).compareTo(BigDecimal.valueOf(30)));
        assertEquals(0, RiskMath.channelBScore(BigDecimal.valueOf(70), 2).compareTo(BigDecimal.valueOf(36)));
        assertEquals(0, RiskMath.channelBScore(BigDecimal.valueOf(110), 0).compareTo(BigDecimal.ZERO));
        assertEquals(0, RiskMath.channelBScore(BigDecimal.valueOf(10), 40).compareTo(BigDecimal.valueOf(100)));
    }

    @Test
    @DisplayName("置信度与输出形态：覆盖率低于 1/3 只给行为预警")
    void confidenceAndOutput() {
        BigDecimal c = RiskMath.confidence(0.75, 1.0, 1.0);
        assertEquals(0, c.compareTo(new BigDecimal("0.750")));
        assertEquals("HIGH", RiskMath.confidenceLevel(c));

        assertEquals("FULL_SCORE", RiskMath.outputMode(c, 0.9));
        assertEquals("BEHAVIOR_ONLY", RiskMath.outputMode(c, 0.2));   // 覆盖率 20%
        assertEquals("SCORE_WITH_NOTE", RiskMath.outputMode(new BigDecimal("0.5"), 0.8));
        assertEquals("BEHAVIOR_ONLY", RiskMath.outputMode(new BigDecimal("0.2"), 0.8));
        assertEquals("INSUFFICIENT", RiskMath.outputMode(new BigDecimal("0.1"), 0.8));
    }

    @Test
    @DisplayName("等级映射：红 75 / 橙 60 / 黄 40")
    void level() {
        BigDecimal red = BigDecimal.valueOf(75), orange = BigDecimal.valueOf(60), yellow = BigDecimal.valueOf(40);
        assertEquals("RED", RiskMath.riskLevel(BigDecimal.valueOf(75), red, orange, yellow));
        assertEquals("ORANGE", RiskMath.riskLevel(BigDecimal.valueOf(60), red, orange, yellow));
        assertEquals("YELLOW", RiskMath.riskLevel(BigDecimal.valueOf(40), red, orange, yellow));
        assertEquals("GREEN", RiskMath.riskLevel(BigDecimal.valueOf(39.9), red, orange, yellow));
    }

    // ============================================================ M16：过程分 / r_final / 绝对口径
    @Test
    @DisplayName("M16 过程分：只算非终结分支，按已录权重归一（0–100）")
    void processScoreExcludesTerminal() {
        Map<Long, BigDecimal> pct = new LinkedHashMap<>();
        pct.put(1L, BigDecimal.valueOf(90));   // 随堂，绝对权重 14
        pct.put(2L, BigDecimal.valueOf(80));   // 项目，8
        pct.put(3L, BigDecimal.valueOf(100));  // 表现，2
        pct.put(4L, BigDecimal.valueOf(95));   // 作业，16
        pct.put(5L, BigDecimal.valueOf(30));   // 期末，60 —— 必须被排除

        RiskMath.ProcessScore ps = RiskMath.processScore(case101(), pct);

        // (90×14 + 80×8 + 100×2 + 95×16) / 100 = 36.2 → 36.2 / 40 × 100 = 90.5
        assertEquals(0, ps.score.compareTo(new BigDecimal("90.50")));
        assertEquals(0, ps.coverage.compareTo(new BigDecimal("1.0000")));
        assertEquals(0, ps.recordedWeight.compareTo(BigDecimal.valueOf(40)));
        assertEquals(0, ps.totalWeight.compareTo(BigDecimal.valueOf(40)));
    }

    @Test
    @DisplayName("M16 过程分：没录的分项不进分母（覆盖率下降，不被一项课堂表现代表）")
    void processScoreSkipsUnavailable() {
        List<RiskMath.SchemeWeight> weights = case101();
        weights.removeIf(w -> w.itemId == 3L);
        weights.add(new RiskMath.SchemeWeight(3L, BigDecimal.valueOf(2), false, false));

        Map<Long, BigDecimal> pct = new LinkedHashMap<>();
        pct.put(1L, BigDecimal.valueOf(90));
        pct.put(2L, BigDecimal.valueOf(80));
        pct.put(4L, BigDecimal.valueOf(95));

        RiskMath.ProcessScore ps = RiskMath.processScore(weights, pct);

        // (90×14 + 80×8 + 95×16) / 100 = 34.2 → 34.2 / 38 × 100 = 90.00
        assertEquals(0, ps.score.compareTo(new BigDecimal("90.00")));
        assertEquals(0, ps.coverage.compareTo(new BigDecimal("0.9500")));
        assertEquals(0, ps.recordedWeight.compareTo(BigDecimal.valueOf(38)));
    }

    @Test
    @DisplayName("M16 过程分：个别学生缺已录分项按 0 分计（分母不缩小）")
    void processScoreCountsStudentAbsenceAsZero() {
        Map<Long, BigDecimal> pct = new LinkedHashMap<>();
        pct.put(1L, BigDecimal.valueOf(90));
        pct.put(2L, BigDecimal.valueOf(80));
        pct.put(4L, BigDecimal.valueOf(95));   // 表现（2 分）缺交

        RiskMath.ProcessScore ps = RiskMath.processScore(case101(), pct);

        assertEquals(0, ps.score.compareTo(new BigDecimal("85.50")));
        assertEquals(0, ps.coverage.compareTo(new BigDecimal("1.0000")));
    }

    @Test
    @DisplayName("M16 过程分：一个可用分项都没有时为 null（页面显示—，不能当 0 分）")
    void processScoreWithoutData() {
        List<RiskMath.SchemeWeight> weights = new ArrayList<>();
        weights.add(new RiskMath.SchemeWeight(1L, BigDecimal.valueOf(14), false, false));
        weights.add(new RiskMath.SchemeWeight(5L, BigDecimal.valueOf(60), true, false));

        RiskMath.ProcessScore ps = RiskMath.processScore(weights, new LinkedHashMap<>());

        assertNull(ps.score);
        assertEquals(0, ps.coverage.compareTo(BigDecimal.ZERO));
        assertEquals(0, ps.totalWeight.compareTo(BigDecimal.valueOf(14)));
    }

    @Test
    @DisplayName("M16 r_final 取终结性根分支占比：101=0.60、103=0.40")
    void finalRatioUsesRootBranch() {
        assertEquals(0, RiskMath.finalRatio(case101()).compareTo(new BigDecimal("0.60")));

        List<RiskMath.SchemeWeight> case103 = new ArrayList<>();
        case103.add(new RiskMath.SchemeWeight(1L, BigDecimal.valueOf(21), false, true));
        case103.add(new RiskMath.SchemeWeight(2L, BigDecimal.valueOf(12), false, true));
        case103.add(new RiskMath.SchemeWeight(3L, BigDecimal.valueOf(24), false, true));
        case103.add(new RiskMath.SchemeWeight(4L, BigDecimal.valueOf(3), false, true));
        case103.add(new RiskMath.SchemeWeight(5L, BigDecimal.valueOf(40), true, true));
        assertEquals(0, RiskMath.finalRatio(case103).compareTo(new BigDecimal("0.40")));
    }

    @Test
    @DisplayName("M16 已得总评分 = 过程分 × (1 − r_final)")
    void estimatedEarned() {
        assertEquals(0, RiskMath.estimatedEarned(new BigDecimal("90.50"), new BigDecimal("0.6"))
                .compareTo(new BigDecimal("36.20")));
        assertEquals(0, RiskMath.estimatedEarned(new BigDecimal("84.70"), new BigDecimal("0.4"))
                .compareTo(new BigDecimal("50.82")));
        assertNull(RiskMath.estimatedEarned(null, new BigDecimal("0.6")));
    }

    @Test
    @DisplayName("M16 绝对风险分：分段映射，及格线为折点（35→0、45→20、55→40、60→50、68→60、80→75、100→100）")
    void absoluteRiskScore() {
        BigDecimal pass = BigDecimal.valueOf(60), full = BigDecimal.valueOf(100);
        assertEquals(0, RiskMath.absoluteRiskScore(BigDecimal.valueOf(30), pass, full).signum());
        assertEquals(0, RiskMath.absoluteRiskScore(BigDecimal.valueOf(35), pass, full).signum());
        assertEquals(0, RiskMath.absoluteRiskScore(new BigDecimal("45"), pass, full).compareTo(new BigDecimal("20")));
        assertEquals(0, RiskMath.absoluteRiskScore(new BigDecimal("55"), pass, full).compareTo(new BigDecimal("40")));
        assertEquals(0, RiskMath.absoluteRiskScore(new BigDecimal("60"), pass, full).compareTo(new BigDecimal("50")));
        assertEquals(0, RiskMath.absoluteRiskScore(new BigDecimal("68"), pass, full).compareTo(new BigDecimal("60")));
        assertEquals(0, RiskMath.absoluteRiskScore(new BigDecimal("80"), pass, full).compareTo(new BigDecimal("75")));
        assertEquals(0, RiskMath.absoluteRiskScore(new BigDecimal("100"), pass, full).compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, RiskMath.absoluteRiskScore(new BigDecimal("150"), pass, full).compareTo(BigDecimal.valueOf(100)));
        assertNull(RiskMath.absoluteRiskScore(null, pass, full));
    }

    @Test
    @DisplayName("M16 教师风格：连续映射，偏严 Δ=−1.61 → ×1.0817，偏松 Δ=+0.19 → ×0.9903")
    void teacherStyleFactor() {
        BigDecimal strict = new BigDecimal("1.05"), loose = new BigDecimal("0.95"), fuse = BigDecimal.valueOf(20);
        // 1.05^1.61 = 1.0817（旧的离散版这里只给 1.05，−0.46 这种干脆给 1.00）
        assertEquals(0, RiskMath.teacherStyleFactor(new BigDecimal("-1.61"), 45, fuse, strict, loose)
                .compareTo(new BigDecimal("1.0817")));
        assertEquals(0, RiskMath.teacherStyleFactor(new BigDecimal("-0.46"), 45, fuse, strict, loose)
                .compareTo(new BigDecimal("1.0227")));
        assertEquals(0, RiskMath.teacherStyleFactor(new BigDecimal("0.19"), 45, fuse, strict, loose)
                .compareTo(new BigDecimal("0.9903")));
        assertEquals(0, RiskMath.teacherStyleFactor(BigDecimal.ZERO, 45, fuse, strict, loose)
                .compareTo(BigDecimal.ONE));
        // 样本 < 30 折半：1 + (1.0817−1)×0.5 = 1.0409
        assertEquals(0, RiskMath.teacherStyleFactor(new BigDecimal("-1.61"), 12, fuse, strict, loose)
                .compareTo(new BigDecimal("1.0409")));
        // |Δ| 超熔断阈值 → 直接不用
        assertEquals(0, RiskMath.teacherStyleFactor(new BigDecimal("-25"), 45, fuse, strict, loose)
                .compareTo(BigDecimal.ONE));
    }

    @Test
    @DisplayName("M16 该科历史挂科率：26.7% → 触到上限 1.20；小样本折半；无数据 1.00")
    void failRateFactor() {
        BigDecimal base = BigDecimal.valueOf(5), step = new BigDecimal("1.05"), cap = new BigDecimal("1.20");
        assertEquals(0, RiskMath.failRateFactor(new BigDecimal("26.70"), 45, base, step, cap)
                .compareTo(new BigDecimal("1.20")));
        assertEquals(0, RiskMath.failRateFactor(new BigDecimal("6.00"), 45, base, step, cap)
                .compareTo(new BigDecimal("1.0100")));
        // 样本 20 人 → 向 1 折半
        assertEquals(0, RiskMath.failRateFactor(new BigDecimal("26.70"), 20, base, step, cap)
                .compareTo(new BigDecimal("1.1000")));
        assertEquals(0, RiskMath.failRateFactor(null, 45, base, step, cap).compareTo(BigDecimal.ONE));
    }

    @Test
    @DisplayName("M16 个人前科：同课重修 ×2、时间衰减、按权重给 prior1/prior2")
    void personalPrior() {
        BigDecimal p1 = new BigDecimal("1.05"), p2 = new BigDecimal("1.10");
        // 同一门课挂过、1 个学期前：权重 2/(1+0.5)=1.333 → prior2
        BigDecimal sameCourse = RiskMath.priorWeight(true, 1);
        assertEquals(0, sameCourse.compareTo(new BigDecimal("1.3333")));
        assertEquals(0, RiskMath.personalPriorFactor(sameCourse, p1, p2).compareTo(p2));
        // 别的课挂过一次、1 个学期前：0.6667 → 无前科（<0.5？不，>0.5 → prior1）
        assertEquals(0, RiskMath.priorWeight(false, 1).compareTo(new BigDecimal("0.6667")));
        assertEquals(0, RiskMath.personalPriorFactor(new BigDecimal("0.6667"), p1, p2).compareTo(p1));
        // 三年前的挂科：0.4 → 衰减到没有影响
        assertEquals(0, RiskMath.priorWeight(false, 3).compareTo(new BigDecimal("0.4000")));
        assertEquals(0, RiskMath.personalPriorFactor(new BigDecimal("0.4000"), p1, p2).compareTo(BigDecimal.ONE));
        assertEquals(0, RiskMath.personalPriorFactor(null, p1, p2).compareTo(BigDecimal.ONE));
    }

    @Test
    @DisplayName("M16 叠加规则取最大：缺课超限 95、不可达 100、再缺即挂 ≥55、剩余 2 次 ≥40")
    void guardedRiskScore() {
        BigDecimal pass = BigDecimal.valueOf(60), full = BigDecimal.valueOf(100);
        assertEquals(0, RiskMath.guardedRiskScore(BigDecimal.valueOf(45), pass, full, true, 3)
                .compareTo(BigDecimal.valueOf(95)));
        assertEquals(0, RiskMath.guardedRiskScore(BigDecimal.valueOf(120), pass, full, false, 4)
                .compareTo(BigDecimal.valueOf(100)));
        // 需考 45 → 分数部分 20（起点 35），门槛地板把它抬起来
        assertEquals(0, RiskMath.guardedRiskScore(BigDecimal.valueOf(45), pass, full, false, 1)
                .compareTo(BigDecimal.valueOf(55)));
        assertEquals(0, RiskMath.guardedRiskScore(BigDecimal.valueOf(45), pass, full, false, 2)
                .compareTo(BigDecimal.valueOf(40)));
        // 需考 68 → 60，已经高于"剩余 2 次"的地板
        assertEquals(0, RiskMath.guardedRiskScore(BigDecimal.valueOf(68), pass, full, false, 2)
                .compareTo(new BigDecimal("60")));
        assertEquals(0, RiskMath.guardedRiskScore(BigDecimal.valueOf(45), pass, full, false, 3)
                .compareTo(new BigDecimal("20")));
    }

    @Test
    @DisplayName("M16 门槛地板分与乘子无关：硬线 95、不可达 100、剩 1 次 55、剩 2 次 40、否则 0")
    void guardFloorIsIndependentOfFactors() {
        assertEquals(0, RiskMath.guardFloor(true, false, 5).compareTo(BigDecimal.valueOf(95)));
        assertEquals(0, RiskMath.guardFloor(false, true, 5).compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, RiskMath.guardFloor(false, false, 0).compareTo(BigDecimal.valueOf(55)));
        assertEquals(0, RiskMath.guardFloor(false, false, 1).compareTo(BigDecimal.valueOf(55)));
        assertEquals(0, RiskMath.guardFloor(false, false, 2).compareTo(BigDecimal.valueOf(40)));
        assertEquals(0, RiskMath.guardFloor(false, false, 3).signum());
        assertEquals(0, RiskMath.guardFloor(false, false, null).signum());
    }

    @Test
    @DisplayName("M16 等级仍由阈值决定（v2.1.0 的 70 / 55 / 35）")
    void levelWithV21Thresholds() {
        BigDecimal red = BigDecimal.valueOf(70), orange = BigDecimal.valueOf(55), yellow = BigDecimal.valueOf(35);
        assertEquals("RED", RiskMath.riskLevel(BigDecimal.valueOf(74), red, orange, yellow));
        assertEquals("ORANGE", RiskMath.riskLevel(BigDecimal.valueOf(55), red, orange, yellow));
        assertEquals("YELLOW", RiskMath.riskLevel(BigDecimal.valueOf(35), red, orange, yellow));
        assertEquals("GREEN", RiskMath.riskLevel(new BigDecimal("34.99"), red, orange, yellow));
    }

    /** 101 的绝对权重样本：随堂 14 / 项目 8 / 表现 2 / 作业 16（非终结）+ 期末 60（终结） */
    private static List<RiskMath.SchemeWeight> case101() {
        List<RiskMath.SchemeWeight> weights = new ArrayList<>();
        weights.add(new RiskMath.SchemeWeight(1L, BigDecimal.valueOf(14), false, true));
        weights.add(new RiskMath.SchemeWeight(2L, BigDecimal.valueOf(8), false, true));
        weights.add(new RiskMath.SchemeWeight(3L, BigDecimal.valueOf(2), false, true));
        weights.add(new RiskMath.SchemeWeight(4L, BigDecimal.valueOf(16), false, true));
        weights.add(new RiskMath.SchemeWeight(5L, BigDecimal.valueOf(60), true, true));
        return weights;
    }
}

