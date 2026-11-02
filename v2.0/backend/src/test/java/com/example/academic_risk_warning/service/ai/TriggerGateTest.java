package com.example.academic_risk_warning.service.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Q1 第三道闸门（触发类型一致性）的单元测试。
 *
 * <p>这条闸门在线上很难自然触发——模型多数时候说的是对的类型；
 * 但一旦串了口径，同一张预警两次生成就会给出互斥结论。所以用单测把判定钉死。</p>
 */
class TriggerGateTest {

    @Test
    @DisplayName("正文命中本类型关键词 → 放行")
    void consistentPasses() {
        assertNull(TriggerGate.mismatch("你这个学期缺勤 6 次，已经超过允许的 4 次。", "ABSENCE"));
        assertNull(TriggerGate.mismatch("你的总分 57.9，处在及格线附近。", "SCORE"));
        assertNull(TriggerGate.mismatch("课后作业缺交 3 次，平时分受影响。", "HOMEWORK"));
        assertNull(TriggerGate.mismatch("三级项目没有按期提交。", "PROJECT"));
        assertNull(TriggerGate.mismatch("期末考试还没出分，卷面占 60%。", "EXAM"));
    }

    @Test
    @DisplayName("缺勤预警却通篇讲分数 → 判为串口径")
    void absentCardTalkingAboutScore() {
        String text = "你的总分 57.9，处在及格线边缘，班内排名靠后，卷面需要提高。";
        assertNotNull(TriggerGate.mismatch(text, "ABSENCE"));
    }

    @Test
    @DisplayName("分数预警却通篇讲考勤 → 判为串口径")
    void scoreCardTalkingAboutAbsence() {
        String text = "你这个学期缺勤 6 次、迟到 3 次，考勤情况需要改善。";
        assertNotNull(TriggerGate.mismatch(text, "SCORE"));
    }

    @Test
    @DisplayName("纯定性描述（两类关键词都没命中）→ 放行，不误伤")
    void purelyQualitativePasses() {
        assertNull(TriggerGate.mismatch("需要你尽快调整学习节奏，多和老师沟通。", "SCORE"));
    }

    @Test
    @DisplayName("MULTI / DATA 不参与一致性校验；空文本判为问题")
    void multiAndDataSkipped() {
        assertNull(TriggerGate.mismatch("多门课程同时亮灯，需要整体安排。", "MULTI"));
        assertNull(TriggerGate.mismatch("本学期数据不足，按当前记录先给提示。", "DATA"));
        assertNotNull(TriggerGate.mismatch("   ", "SCORE"));
    }
}
