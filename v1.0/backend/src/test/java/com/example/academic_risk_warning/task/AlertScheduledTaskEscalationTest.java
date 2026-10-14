package com.example.academic_risk_warning.task;

import com.example.academic_risk_warning.config.WarningSystemProperties;
import com.example.academic_risk_warning.entity.AlertRecord;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.TeacherClass;
import com.example.academic_risk_warning.mapper.AlertRecordMapper;
import com.example.academic_risk_warning.mapper.NotificationMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.TeacherClassMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.service.NotificationService;
import com.example.academic_risk_warning.service.WarningGenerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 超时升级通知幂等测试：
 * 定时任务每天都会扫到同一批超时预警，同一件事（预警ID + 类型 + 标题）只应通知一次。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("超时升级通知幂等测试")
class AlertScheduledTaskEscalationTest {

    private static final Long STUDENT_ID = 21L;
    private static final Long TEACHER_ID = 5L;

    @Mock private WarningGenerationService warningGenerationService;
    @Mock private AlertRecordMapper alertRecordMapper;
    @Mock private TeacherMapper teacherMapper;
    @Mock private NotificationService notificationService;
    @Mock private StudentMapper studentMapper;
    @Mock private TeacherClassMapper teacherClassMapper;
    @Mock private NotificationMapper notificationMapper;
    @Mock private WarningSystemProperties warningProperties;

    private AlertScheduledTask task() {
        return new AlertScheduledTask(warningGenerationService, alertRecordMapper, teacherMapper, notificationService,
                studentMapper, teacherClassMapper, notificationMapper, warningProperties);
    }

    private void stubCommon() {
        when(warningProperties.getEscalateActiveFirstDays()).thenReturn(2);
        when(warningProperties.getEscalateActiveSecondDays()).thenReturn(5);
        when(warningProperties.getEscalateAckedDays()).thenReturn(3);
        when(warningProperties.getEscalateHandledFirstDays()).thenReturn(3);
        when(warningProperties.getEscalateHandledUpgradeDays()).thenReturn(5);

        Student student = new Student();
        student.setId(STUDENT_ID);
        student.setStudentName("测试学生");
        student.setClassName("测试1班");
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(student);

        TeacherClass teacherClass = new TeacherClass();
        teacherClass.setTeacherId(TEACHER_ID);
        teacherClass.setClassName("测试1班");
        when(teacherClassMapper.selectList(any())).thenReturn(List.of(teacherClass));
    }

    @Test
    @DisplayName("同一条超时预警只催办一次，第二次扫描跳过")
    void shouldNotNotifyTwiceForSameAlert() {
        stubCommon();

        AlertRecord overdue = new AlertRecord();
        overdue.setId(1L);
        overdue.setStudentId(STUDENT_ID);
        overdue.setCourseId(1L);
        overdue.setStatus("ACTIVE");
        overdue.setAlertLevel("YELLOW");
        overdue.setRiskScore(BigDecimal.valueOf(20));
        overdue.setCreateTime(LocalDateTime.now().minusDays(3));

        // 每轮 escalateTimeouts 会查 5 次（1档/2档/已确认/已处理/待升级）：
        // 只有每轮的第 1 次命中"超时未处理"的预警，其余返回空
        AtomicLong selectListCalls = new AtomicLong();
        when(alertRecordMapper.selectList(any())).thenAnswer(invocation ->
                selectListCalls.getAndIncrement() % 5 == 0 ? List.of(overdue) : List.of());

        // 第一次扫描：还没有催办记录；第二次扫描：已存在同标题通知
        when(notificationMapper.selectCount(any())).thenReturn(0L, 1L);

        AlertScheduledTask task = task();
        task.escalateTimeouts();
        task.escalateTimeouts();

        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(1)).create(eq(TEACHER_ID), eq("TEACHER"),
                titleCaptor.capture(), anyString(), eq(1L), eq("TIMEOUT"));
        assertTrue(titleCaptor.getValue().contains("已超2天"),
                "催办标题应包含配置的 2 天阈值，实际：" + titleCaptor.getValue());
    }
}
