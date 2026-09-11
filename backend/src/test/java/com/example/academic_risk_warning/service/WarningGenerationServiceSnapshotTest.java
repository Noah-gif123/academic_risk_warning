package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.config.WarningSystemProperties;
import com.example.academic_risk_warning.entity.AlertRecord;
import com.example.academic_risk_warning.entity.AlertSnapshot;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.StudentCourse;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 方案A（每日全量快照）回归测试：
 * <ol>
 *   <li>未触发预警的学生同样写入当天快照（GREEN 也记录）；</li>
 *   <li>一人一课一天一条：写入前先清理当天旧记录，重复执行不产生重复行；</li>
 *   <li>无选课记录的学生不写快照；</li>
 *   <li>当天有预警时回填 relatedAlertId，并把 isGeneratedAlert 置为 true。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("每日全量快照生成测试")
class WarningGenerationServiceSnapshotTest {

    private static final Long STUDENT_ID = 7L;
    private static final Long COURSE_ID = 1L;

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

    /** 共享桩：学生选了 1 门课，各维度数据为空（最典型的"无预警"学生） */
    private void stubStudentData(List<AlertRecord> todayAlerts) {
        StudentCourse enrollment = new StudentCourse();
        enrollment.setStudentId(STUDENT_ID);
        enrollment.setCourseId(COURSE_ID);
        when(studentCourseMapper.selectList(any())).thenReturn(List.of(enrollment));

        Course course = new Course();
        course.setId(COURSE_ID);
        course.setCourseName("测试课程");
        when(courseMapper.selectList(any())).thenReturn(List.of(course));

        // 规则配置缺失 → 使用硬编码兜底配置
        when(alertRuleConfigMapper.selectOne(any())).thenReturn(null);

        when(scoreInfoMapper.selectList(any())).thenReturn(List.of());
        when(homeworkInfoMapper.selectList(any())).thenReturn(List.of());
        when(classPerformanceMapper.selectList(any())).thenReturn(List.of());
        when(knowledgeMasteryMapper.selectList(any())).thenReturn(List.of());
        when(historyRiskMapper.selectList(any())).thenReturn(List.of());
        when(studyDurationMapper.selectList(any())).thenReturn(List.of());
        when(alertRecordMapper.selectList(any())).thenReturn(todayAlerts);
        when(alertSnapshotMapper.delete(any())).thenReturn(1);
        when(alertSnapshotMapper.insert(any(AlertSnapshot.class))).thenReturn(1);
    }

    @Test
    @DisplayName("没有预警的学生也会写入当天快照")
    void shouldSnapshotStudentWithoutAlert() {
        stubStudentData(List.of());

        int count = service().snapshotForStudents(List.of(student()));

        assertEquals(1, count);
        verify(alertSnapshotMapper).delete(any());
        ArgumentCaptor<AlertSnapshot> captor = ArgumentCaptor.forClass(AlertSnapshot.class);
        verify(alertSnapshotMapper).insert(captor.capture());
        AlertSnapshot snap = captor.getValue();
        assertEquals(STUDENT_ID, snap.getStudentId());
        assertEquals(COURSE_ID, snap.getCourseId());
        assertEquals(LocalDate.now(), snap.getSnapshotDate());
        assertNotNull(snap.getSnapshotWeek());
        assertNotNull(snap.getRiskScore());
        assertNotNull(snap.getAlertLevel());
        // 无任何预警类型 → 即使风险分不为零也标记为"未触发预警"
        assertEquals(Boolean.FALSE, snap.getIsGeneratedAlert());
        assertNull(snap.getAlertTypes());
    }

    @Test
    @DisplayName("重复执行先清理当天旧记录，不会产生重复行")
    void shouldCleanTodayRowsBeforeInsert() {
        stubStudentData(List.of());

        service().snapshotForStudents(List.of(student()));
        service().snapshotForStudents(List.of(student()));

        verify(alertSnapshotMapper, times(2)).delete(any());
        verify(alertSnapshotMapper, times(2)).insert(any(AlertSnapshot.class));
    }

    @Test
    @DisplayName("没有选课记录的学生不写快照")
    void shouldSkipStudentWithoutCourse() {
        // 无选课 → batchLoadCourses 直接返回空 Map，不会去查课程表
        when(studentCourseMapper.selectList(any())).thenReturn(List.of());
        when(alertSnapshotMapper.delete(any())).thenReturn(0);

        int count = service().snapshotForStudents(List.of(student()));

        assertEquals(0, count);
        verify(alertSnapshotMapper, never()).insert(any(AlertSnapshot.class));
    }

    @Test
    @DisplayName("当天已有预警时回填 relatedAlertId 并标记 isGeneratedAlert")
    void shouldLinkTodayAlert() {
        AlertRecord todayAlert = new AlertRecord();
        todayAlert.setId(999L);
        todayAlert.setStudentId(STUDENT_ID);
        todayAlert.setCourseId(COURSE_ID);
        todayAlert.setAlertType("FAILURE");
        stubStudentData(List.of(todayAlert));

        service().snapshotForStudents(List.of(student()));

        ArgumentCaptor<AlertSnapshot> captor = ArgumentCaptor.forClass(AlertSnapshot.class);
        verify(alertSnapshotMapper).insert(captor.capture());
        assertEquals(999L, captor.getValue().getRelatedAlertId());
        assertEquals(Boolean.TRUE, captor.getValue().getIsGeneratedAlert());
    }

    @Test
    @DisplayName("学生列表为空时直接返回 0")
    void shouldReturnZeroForEmptyList() {
        assertEquals(0, service().snapshotForStudents(List.of()));
        verifyNoInteractions(alertSnapshotMapper);
    }

    private Student student() {
        Student student = new Student();
        student.setId(STUDENT_ID);
        student.setStudentName("测试学生");
        student.setGrade("大一上");
        student.setClassName("测试1班");
        return student;
    }
}
