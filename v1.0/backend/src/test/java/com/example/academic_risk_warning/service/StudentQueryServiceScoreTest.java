package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.entity.HistoryRisk;
import com.example.academic_risk_warning.entity.KnowledgeMastery;
import com.example.academic_risk_warning.vo.ScoreInfoVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 老生通用版综合评分口径回归测试。
 *
 * <p>覆盖四个已修复的口径问题：
 * <ol>
 *   <li>挂科史字段为"是/否"时同样要扣分（原先 Integer.parseInt("是") 失败导致不扣分）；</li>
 *   <li>知识点正确率使用 (总题数 - 错题数) / 总题数，而不是 basicCorrect / 总题数；</li>
 *   <li>期末成绩缺失时用 平时 40% + 期中 60% 推算，而不是直接记 0 分；</li>
 *   <li>缺失历史记录时按默认历史风险扣分，不再等于"历史零风险"。</li>
 * </ol>
 */
@DisplayName("学生综合评分口径回归测试")
class StudentQueryServiceScoreTest {

    private static final Long COURSE_ID = 1L;

    @Test
    @DisplayName("挂科史=\"是\" 时历史风险部分扣 20 分")
    void seniorScoreShouldPenalizeFailedHistory() {
        ScoreInfoVO score = score(80, null, null);
        HistoryRisk risk = history(COURSE_ID, "是", "稳定");

        Double total = StudentQueryService.calculateSeniorScore(
                COURSE_ID, score, List.of(), List.of(), List.of(), List.of(risk));

        // 学业 80 * 0.26 = 20.8；历史 (100 - 20) * 0.45 = 36.0
        assertEquals(56.8, total, 0.05);
    }

    @Test
    @DisplayName("挂科史为数字时按次数扣分")
    void seniorScoreShouldSupportNumericFailedCount() {
        ScoreInfoVO score = score(80, null, null);
        HistoryRisk risk = history(COURSE_ID, "2", "稳定");

        Double total = StudentQueryService.calculateSeniorScore(
                COURSE_ID, score, List.of(), List.of(), List.of(), List.of(risk));

        // 学业 20.8；历史 (100 - 40) * 0.45 = 27.0
        assertEquals(47.8, total, 0.05);
    }

    @Test
    @DisplayName("知识点正确率按 (总题数 - 错题数) / 总题数 计算")
    void seniorScoreShouldUseOverallCorrectRate() {
        ScoreInfoVO score = score(80, null, null);
        KnowledgeMastery km = new KnowledgeMastery();
        km.setCourseId(COURSE_ID);
        km.setTotalQuestion(10);
        km.setErrorCount(4);
        km.setBasicTotal(10);
        km.setBasicCorrect(2); // 旧实现取该值会得到 20 分，修复后应取 60 分

        Double total = StudentQueryService.calculateSeniorScore(
                COURSE_ID, score, List.of(), List.of(), List.of(km), List.of(history(COURSE_ID, "否", "稳定")));

        // 学业 20.8 + 知识点 60 * 0.09 = 5.4 + 历史 100 * 0.45 = 45.0
        assertEquals(71.2, total, 0.05);
    }

    @Test
    @DisplayName("期末成绩缺失时用 平时40% + 期中60% 推算")
    void seniorScoreShouldFallbackWhenFinalMissing() {
        ScoreInfoVO score = score(90, 50, null);

        Double total = StudentQueryService.calculateSeniorScore(
                COURSE_ID, score, List.of(), List.of(), List.of(), List.of(history(COURSE_ID, "否", "稳定")));

        // (90 * 0.4 + 50 * 0.6) = 66 -> 66 * 0.26 = 17.16 -> 17.2；再加历史 45.0
        assertEquals(62.2, total, 0.05);
    }

    @Test
    @DisplayName("缺失历史记录时按默认历史风险 15 分计，而不是满分")
    void seniorHistoryScoreShouldApplyDefaultRiskWhenMissing() {
        assertEquals(85.0, StudentQueryService.seniorHistoryScore(null), 0.001);
    }

    @Test
    @DisplayName("历史得分：挂科/不稳定/正常 三种情况")
    void seniorHistoryScoreShouldHandleStability() {
        assertEquals(80.0, StudentQueryService.seniorHistoryScore(history(COURSE_ID, "是", "稳定")), 0.001);
        assertEquals(60.0, StudentQueryService.seniorHistoryScore(history(COURSE_ID, "2", null)), 0.001);
        assertEquals(85.0, StudentQueryService.seniorHistoryScore(history(COURSE_ID, "否", "不稳定")), 0.001);
        assertEquals(100.0, StudentQueryService.seniorHistoryScore(history(COURSE_ID, "否", "稳定")), 0.001);
    }

    @Test
    @DisplayName("完全没有学业数据时不抛异常")
    void seniorScoreShouldTolerateMissingData() {
        Double total = StudentQueryService.calculateSeniorScore(
                COURSE_ID, score(null, null, null), List.of(), List.of(), List.of(),
                List.of(history(COURSE_ID, "否", "稳定")));

        assertEquals(45.0, total, 0.001);
    }

    @Test
    @DisplayName("parseFailedCount 兼容 是/否/数字/空值")
    void parseFailedCountShouldBeTolerant() {
        assertEquals(1, StudentQueryService.parseFailedCount("是"));
        assertEquals(0, StudentQueryService.parseFailedCount("否"));
        assertEquals(2, StudentQueryService.parseFailedCount("2"));
        assertEquals(0, StudentQueryService.parseFailedCount(null));
        assertEquals(0, StudentQueryService.parseFailedCount("未知"));
    }

    private ScoreInfoVO score(Integer usual, Integer mid, Integer fin) {
        ScoreInfoVO vo = new ScoreInfoVO();
        vo.setCourseId(COURSE_ID);
        vo.setUsualScore(usual);
        vo.setMidScore(mid);
        vo.setFinalScore(fin);
        return vo;
    }

    private HistoryRisk history(Long courseId, String lastTermFailed, String studyStable) {
        HistoryRisk risk = new HistoryRisk();
        risk.setCourseId(courseId);
        risk.setLastTermFailed(lastTermFailed);
        risk.setStudyStable(studyStable);
        return risk;
    }
}
