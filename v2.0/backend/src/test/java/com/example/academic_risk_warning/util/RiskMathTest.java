package com.example.academic_risk_warning.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
}
