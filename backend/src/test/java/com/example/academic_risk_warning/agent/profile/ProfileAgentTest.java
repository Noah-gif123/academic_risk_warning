package com.example.academic_risk_warning.agent.profile;

import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.core.AgentResult;
import com.example.academic_risk_warning.entity.AlertSnapshot;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.StudentProfile;
import com.example.academic_risk_warning.llm.LLMClient;
import com.example.academic_risk_warning.mapper.AlertRecordMapper;
import com.example.academic_risk_warning.mapper.AlertSnapshotMapper;
import com.example.academic_risk_warning.mapper.CourseKnowledgePointMapper;
import com.example.academic_risk_warning.mapper.ExerciseMapper;
import com.example.academic_risk_warning.mapper.ExerciseRecommendationMapper;
import com.example.academic_risk_warning.mapper.KnowledgeMasteryMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.StudentProfileMapper;
import com.example.academic_risk_warning.mapper.StudentWeakPointMapper;
import com.example.academic_risk_warning.service.StudentGoalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProfileAgent（学情三维画像）回归测试：
 * 1) 快照"首次/最新"取值方向正确（曾因下标写反而颠倒）；
 * 2) LLM 未返回合法 JSON 时失败且不落库脏数据；
 * 3) 画像按 (学生, 课程) 维度持久化。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("学情画像 ProfileAgent 回归测试")
class ProfileAgentTest {

    private static final Long STUDENT_ID = 7L;
    private static final Long COURSE_ID = 2L;

    @Mock private LLMClient llmClient;
    @Mock private StudentWeakPointMapper weakPointMapper;
    @Mock private KnowledgeMasteryMapper masteryMapper;
    @Mock private ExerciseRecommendationMapper recommendMapper;
    @Mock private ExerciseMapper exerciseMapper;
    @Mock private AlertRecordMapper alertRecordMapper;
    @Mock private AlertSnapshotMapper snapshotMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private CourseKnowledgePointMapper kpMapper;
    @Mock private StudentProfileMapper studentProfileMapper;
    @Mock private StudentGoalService studentGoalService;
    @Mock private com.example.academic_risk_warning.service.StudentMemoryService studentMemoryService;

    private ProfileAgent newAgent() {
        return new ProfileAgent(llmClient, weakPointMapper, masteryMapper, recommendMapper, exerciseMapper,
                alertRecordMapper, snapshotMapper, studentMapper, kpMapper, studentProfileMapper, studentGoalService,
                new com.example.academic_risk_warning.config.AgentProperties(), studentMemoryService);
    }

    private void stubCommonQueries(List<AlertSnapshot> snapshots) {
        Student student = new Student();
        student.setId(STUDENT_ID);
        student.setStudentName("测试学生");
        when(studentMapper.selectById(STUDENT_ID)).thenReturn(student);
        when(weakPointMapper.selectList(any())).thenReturn(List.of());
        when(masteryMapper.selectOne(any())).thenReturn(null);
        when(recommendMapper.selectList(any())).thenReturn(List.of());
        when(snapshotMapper.selectList(any())).thenReturn(snapshots);
        when(alertRecordMapper.selectOne(any())).thenReturn(null);
        when(studentGoalService.getByStudentAndCourse(STUDENT_ID, COURSE_ID)).thenReturn(null);
    }

    private AgentContext context() {
        AgentContext ctx = new AgentContext(STUDENT_ID);
        ctx.setCourseId(COURSE_ID);
        return ctx;
    }

    @Test
    @DisplayName("快照按日期升序时，首次=最早、最新=最近，不能颠倒")
    void promptShouldLabelEarliestSnapshotAsFirst() {
        stubCommonQueries(List.of(snapshot("2026-03-01", 10, 55), snapshot("2026-03-08", 90, 40)));

        String prompt = newAgent().buildUserPrompt(context());

        assertTrue(prompt.contains("首次风险分=10.0"), prompt);
        assertTrue(prompt.contains("最新风险分=90.0"), prompt);
        assertTrue(prompt.contains("风险分变化(最新-首次)=+80.0"), prompt);
        assertTrue(prompt.contains("风险上升"), prompt);
    }

    @Test
    @DisplayName("LLM 返回非 JSON 时应判定失败且不写库")
    void executeShouldFailAndNotPersistWhenLlmOutputInvalid() {
        stubCommonQueries(List.of());
        when(llmClient.chat(anyString(), anyString())).thenReturn("抱歉，我无法生成画像。");

        AgentResult<Map<String, Object>> result = newAgent().execute(context());

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("未返回三维画像"), result.getErrorMessage());
        verify(studentProfileMapper, never()).insert(any(StudentProfile.class));
    }

    @Test
    @DisplayName("画像按 (学生, 课程) 持久化并带回 courseId")
    void executeShouldPersistProfileWithCourseId() {
        stubCommonQueries(List.of());
        when(llmClient.chat(anyString(), anyString())).thenReturn("""
                {"knowledgeProfile":{"level":"一般"},
                 "habitProfile":{"level":"规律型"},
                 "goalProfile":{"level":"稳定"}}
                """);
        when(studentProfileMapper.selectOne(any())).thenReturn(null);

        AgentResult<Map<String, Object>> result = newAgent().execute(context());

        assertTrue(result.isSuccess(), result.getErrorMessage());

        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(studentProfileMapper).insert(captor.capture());
        StudentProfile saved = captor.getValue();
        assertEquals(STUDENT_ID, saved.getStudentId());
        assertEquals(COURSE_ID, saved.getCourseId());
        assertNotNull(saved.getKnowledgeProfile());
        assertNotNull(saved.getHabitProfile());
        assertNotNull(saved.getGoalProfile());

        assertEquals(COURSE_ID, result.getData().get("courseId"));
        assertEquals(Boolean.TRUE, result.getData().get("courseScoped"));
    }

    private AlertSnapshot snapshot(String date, int riskScore, int predictedScore) {
        AlertSnapshot snapshot = new AlertSnapshot();
        snapshot.setStudentId(STUDENT_ID);
        snapshot.setCourseId(COURSE_ID);
        snapshot.setSnapshotDate(LocalDate.parse(date));
        snapshot.setAlertLevel("ORANGE");
        snapshot.setRiskScore(BigDecimal.valueOf(riskScore));
        snapshot.setPredictedScore(BigDecimal.valueOf(predictedScore));
        return snapshot;
    }
}
