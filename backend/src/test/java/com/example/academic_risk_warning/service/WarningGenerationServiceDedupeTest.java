package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.config.WarningSystemProperties;
import com.example.academic_risk_warning.entity.AlertRecord;
import com.example.academic_risk_warning.mapper.AlertRecordMapper;
import com.example.academic_risk_warning.mapper.AlertRuleConfigMapper;
import com.example.academic_risk_warning.mapper.AlertSnapshotMapper;
import com.example.academic_risk_warning.mapper.ClassPerformanceMapper;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.HistoryRiskMapper;
import com.example.academic_risk_warning.mapper.HomeworkInfoMapper;
import com.example.academic_risk_warning.mapper.KnowledgeMasteryMapper;
import com.example.academic_risk_warning.mapper.ScoreInfoMapper;
import com.example.academic_risk_warning.mapper.StudentCourseMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.StudyDurationMapper;
import com.example.academic_risk_warning.mapper.TeacherClassMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预警生成去重规则回归测试：
 * <ol>
 *   <li>当天已生成且等级未恶化 → 跳过（修复重复点击"生成预警"导致条数翻倍）；</li>
 *   <li>同一天等级恶化 → 仍然生成；</li>
 *   <li>隔天（昨天生成）→ 次日正常重新评估；</li>
 *   <li>归档旧预警时不要归档当天生成的，避免待处理(ACTIVE)预警被误归档。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("预警生成去重规则测试")
class WarningGenerationServiceDedupeTest {

    private static final Long STUDENT_ID = 12L;

    @Mock private AlertRecordMapper alertRecordMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private ScoreInfoMapper scoreInfoMapper;
    @Mock private HomeworkInfoMapper homeworkInfoMapper;
    @Mock private ClassPerformanceMapper classPerformanceMapper;
    @Mock private KnowledgeMasteryMapper knowledgeMasteryMapper;
    @Mock private HistoryRiskMapper historyRiskMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private StudentCourseMapper studentCourseMapper;
    @Mock private TeacherClassMapper teacherClassMapper;
    @Mock private AlertRuleConfigMapper alertRuleConfigMapper;
    @Mock private StudyDurationMapper studyDurationMapper;
    @Mock private AlertSnapshotMapper alertSnapshotMapper;
    @Mock private NotificationService notificationService;
    @Mock private WarningSystemProperties warningProperties;

    private WarningGenerationService service() {
        return new WarningGenerationService(alertRecordMapper, studentMapper, scoreInfoMapper, homeworkInfoMapper,
                classPerformanceMapper, knowledgeMasteryMapper, historyRiskMapper, courseMapper, studentCourseMapper,
                teacherClassMapper, alertRuleConfigMapper, studyDurationMapper, alertSnapshotMapper,
                notificationService, warningProperties);
    }

    @Test
    @DisplayName("当天已生成且等级未恶化 → 跳过")
    void shouldSkipSameDayAlert() {
        AlertRecord today = alert(1L, LocalDateTime.now().minusHours(1), "ACTIVE", "YELLOW");

        assertTrue(service().shouldSkipGeneration(today, "YELLOW", List.of("FAILURE")));
    }

    @Test
    @DisplayName("当天已生成但已被归档（重复执行场景）→ 仍然跳过")
    void shouldSkipSameDayArchivedAlert() {
        AlertRecord todayArchived = alert(1L, LocalDateTime.now().minusMinutes(5), "ARCHIVED", "YELLOW");

        assertTrue(service().shouldSkipGeneration(todayArchived, "YELLOW", List.of("FAILURE")));
    }

    @Test
    @DisplayName("同一天等级恶化（YELLOW→ORANGE）→ 不跳过，需要重新生成")
    void shouldNotSkipWhenWorsenedSameDay() {
        AlertRecord today = alert(1L, LocalDateTime.now().minusHours(1), "ACTIVE", "YELLOW");

        assertFalse(service().shouldSkipGeneration(today, "ORANGE", List.of("FAILURE")));
    }

    @Test
    @DisplayName("昨天生成的预警 → 次日正常重新评估")
    void shouldNotSkipYesterdayAlert() {
        AlertRecord yesterday = alert(1L, LocalDateTime.now().minusDays(1), "ARCHIVED", "YELLOW");

        assertFalse(service().shouldSkipGeneration(yesterday, "YELLOW", List.of("FAILURE")));
    }

    @Test
    @DisplayName("首次生成（无历史预警）→ 不跳过")
    void shouldNotSkipFirstGeneration() {
        assertFalse(service().shouldSkipGeneration(null, "YELLOW", List.of("FAILURE")));
    }

    @Test
    @DisplayName("归档旧预警时保留当天生成的，只归档此前的")
    void archiveShouldKeepTodayAlerts() {
        AlertRecord todayActive = alert(1L, LocalDateTime.now().minusMinutes(10), "ACTIVE", "YELLOW");
        AlertRecord oldActive = alert(2L, LocalDateTime.now().minusDays(3), "ACTIVE", "YELLOW");
        when(alertRecordMapper.selectList(any())).thenReturn(List.of(todayActive, oldActive));

        service().archiveOldAlerts(STUDENT_ID);

        ArgumentCaptor<AlertRecord> captor = ArgumentCaptor.forClass(AlertRecord.class);
        verify(alertRecordMapper, times(1)).updateById(captor.capture());
        assertEquals(2L, captor.getValue().getId(), "只应归档旧的那条");
        assertEquals("ACTIVE", todayActive.getStatus(), "当天生成的预警必须保持 ACTIVE");
        assertEquals("ARCHIVED", oldActive.getStatus());
    }

    private AlertRecord alert(Long id, LocalDateTime createTime, String status, String level) {
        AlertRecord record = new AlertRecord();
        record.setId(id);
        record.setStudentId(STUDENT_ID);
        record.setCourseId(1L);
        record.setAlertType("FAILURE");
        record.setAlertLevel(level);
        record.setStatus(status);
        record.setRiskScore(BigDecimal.valueOf(20));
        record.setCreateTime(createTime);
        return record;
    }
}
