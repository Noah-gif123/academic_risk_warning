package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.entity.AlertRecord;
import com.example.academic_risk_warning.entity.AlertSnapshot;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.mapper.AlertOperationLogMapper;
import com.example.academic_risk_warning.mapper.AlertRecordMapper;
import com.example.academic_risk_warning.mapper.AlertSnapshotMapper;
import com.example.academic_risk_warning.mapper.CourseKnowledgePointMapper;
import com.example.academic_risk_warning.mapper.ExerciseKnowledgePointMapper;
import com.example.academic_risk_warning.mapper.ExerciseMapper;
import com.example.academic_risk_warning.mapper.ExerciseRecommendationMapper;
import com.example.academic_risk_warning.mapper.InterventionRecordMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.StudentWeakPointMapper;
import com.example.academic_risk_warning.mapper.TeacherClassMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 风险雷达取数优先级测试：
 * <ol>
 *   <li>有有效预警 → 用预警（source=ALERT）；</li>
 *   <li>没有有效预警但有每日快照 → 回退快照（source=SNAPSHOT），不再把五维填成 0；</li>
 *   <li>两者都没有 → hasData=false、source=NONE（前端显示"暂无数据"而不是 0 分画像）。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("风险雷达取数优先级测试")
class AlertQueryServiceRadarTest {

    private static final Long STUDENT_ID = 42L;
    private static final Long COURSE_ID = 1L;

    @Mock private AlertRecordMapper alertRecordMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private TeacherClassMapper teacherClassMapper;
    @Mock private AlertOperationLogMapper alertOperationLogMapper;
    @Mock private InterventionRecordMapper interventionRecordMapper;
    @Mock private AlertSnapshotMapper alertSnapshotMapper;
    @Mock private NotificationService notificationService;
    @Mock private ExerciseMapper exerciseMapper;
    @Mock private ExerciseRecommendationMapper recommendMapper;
    @Mock private ExerciseKnowledgePointMapper exerciseKpMapper;
    @Mock private CourseKnowledgePointMapper kpMapper;
    @Mock private StudentWeakPointMapper weakPointMapper;
    @Mock private ExerciseService exerciseService;

    private AlertQueryService service() {
        return new AlertQueryService(alertRecordMapper, studentMapper, teacherClassMapper, alertOperationLogMapper,
                interventionRecordMapper, alertSnapshotMapper, notificationService, exerciseMapper, recommendMapper,
                exerciseKpMapper, kpMapper, weakPointMapper, exerciseService);
    }

    @Test
    @DisplayName("没有有效预警时回退到每日快照（不再填 0）")
    void shouldFallbackToSnapshotWhenNoAlert() {
        when(alertRecordMapper.selectOne(any())).thenReturn(null);
        when(alertSnapshotMapper.selectOne(any())).thenReturn(snapshot());
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(studentWithoutClass());

        Map<String, Object> radar = service().getStudentRadar(STUDENT_ID, COURSE_ID);

        assertEquals("SNAPSHOT", radar.get("source"));
        assertEquals("COURSE", radar.get("scope"));
        assertTrue(String.valueOf(radar.get("scopeText")).contains("快照"), String.valueOf(radar.get("scopeText")));

        Map<String, Object> stu = studentMap(radar);
        assertEquals(Boolean.TRUE, stu.get("hasData"));
        assertEquals(35.5, (Double) stu.get("academic"), 0.001);
        assertEquals(17.1, (Double) stu.get("homework"), 0.001);
        assertEquals(12.6, (Double) stu.get("attendance"), 0.001);
        assertEquals(30.9, (Double) stu.get("knowledge"), 0.001);
        assertEquals(0.0, (Double) stu.get("history"), 0.001);
        assertEquals("YELLOW", stu.get("alertLevel"));
        assertEquals("SNAPSHOT", stu.get("status"));
        assertNotNull(stu.get("dataDate"));
        assertEquals(0, new BigDecimal("24.8").compareTo((BigDecimal) stu.get("riskScore")));
    }

    @Test
    @DisplayName("有有效预警时优先使用预警数据，不读快照")
    void shouldPreferValidAlert() {
        when(alertRecordMapper.selectOne(any())).thenReturn(alertRecord());
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(studentWithoutClass());

        Map<String, Object> radar = service().getStudentRadar(STUDENT_ID, COURSE_ID);

        assertEquals("ALERT", radar.get("source"));
        assertEquals("COURSE", radar.get("scope"));
        assertTrue(String.valueOf(radar.get("scopeText")).contains("预警"));

        Map<String, Object> stu = studentMap(radar);
        assertEquals(Boolean.TRUE, stu.get("hasData"));
        assertEquals(10.0, (Double) stu.get("academic"), 0.001);
        assertEquals(50.0, (Double) stu.get("history"), 0.001);
        assertEquals("ORANGE", stu.get("alertLevel"));
        assertEquals("ACTIVE", stu.get("status"));

        verifyNoInteractions(alertSnapshotMapper);
    }

    @Test
    @DisplayName("既无预警也无快照时明确标记无数据，而不是给 0 分画像")
    void shouldReportNoDataWhenNothingExists() {
        when(alertRecordMapper.selectOne(any())).thenReturn(null);
        when(alertSnapshotMapper.selectOne(any())).thenReturn(null);
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(studentWithoutClass());

        Map<String, Object> radar = service().getStudentRadar(STUDENT_ID, COURSE_ID);

        assertEquals("NONE", radar.get("source"));
        assertEquals("NONE", radar.get("scope"));

        Map<String, Object> stu = studentMap(radar);
        assertEquals(Boolean.FALSE, stu.get("hasData"));
        assertEquals(0.0, (Double) stu.get("academic"), 0.001);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> studentMap(Map<String, Object> radar) {
        return (Map<String, Object>) radar.get("student");
    }

    /** className 为 null → 跳过班级均值分支，专注验证取数优先级 */
    private Student studentWithoutClass() {
        Student student = new Student();
        student.setId(STUDENT_ID);
        student.setStudentName("测试学生");
        return student;
    }

    private AlertSnapshot snapshot() {
        AlertSnapshot snap = new AlertSnapshot();
        snap.setId(1L);
        snap.setStudentId(STUDENT_ID);
        snap.setCourseId(COURSE_ID);
        snap.setSnapshotDate(LocalDate.now());
        snap.setAlertLevel("YELLOW");
        snap.setRiskScore(new BigDecimal("24.8"));
        snap.setAcademicRiskScore(new BigDecimal("35.5"));
        snap.setHomeworkRiskScore(new BigDecimal("17.1"));
        snap.setAttendanceRiskScore(new BigDecimal("12.6"));
        snap.setKnowledgeRiskScore(new BigDecimal("30.9"));
        snap.setHistoryRiskScore(BigDecimal.ZERO);
        return snap;
    }

    private AlertRecord alertRecord() {
        AlertRecord alert = new AlertRecord();
        alert.setId(9L);
        alert.setStudentId(STUDENT_ID);
        alert.setCourseId(COURSE_ID);
        alert.setAlertLevel("ORANGE");
        alert.setStatus("ACTIVE");
        alert.setRiskScore(new BigDecimal("30.0"));
        alert.setAcademicRiskScore(new BigDecimal("10.0"));
        alert.setHomeworkRiskScore(new BigDecimal("20.0"));
        alert.setAttendanceRiskScore(new BigDecimal("30.0"));
        alert.setKnowledgeRiskScore(new BigDecimal("40.0"));
        alert.setHistoryRiskScore(new BigDecimal("50.0"));
        return alert;
    }
}
