package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.entity.AlertRecord;
import com.example.academic_risk_warning.entity.InterventionRecord;
import com.example.academic_risk_warning.entity.StrategyRecord;
import com.example.academic_risk_warning.entity.StudentGoal;
import com.example.academic_risk_warning.entity.StudentMemory;
import com.example.academic_risk_warning.entity.StudentProfile;
import com.example.academic_risk_warning.entity.StudyPlan;
import com.example.academic_risk_warning.mapper.AlertRecordMapper;
import com.example.academic_risk_warning.mapper.InterventionRecordMapper;
import com.example.academic_risk_warning.mapper.StrategyRecordMapper;
import com.example.academic_risk_warning.mapper.StudentGoalMapper;
import com.example.academic_risk_warning.mapper.StudentMemoryMapper;
import com.example.academic_risk_warning.mapper.StudentProfileMapper;
import com.example.academic_risk_warning.mapper.StudyPlanMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * W3 长期记忆服务测试：规则化压缩出的记忆文本要包含画像/策略/目标等关键信息，
 * 并做长度裁剪；版本号自增；没有历史记忆时注入文本为 null（不污染 prompt）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("学生长期记忆服务测试")
class StudentMemoryServiceTest {

    private static final Long STUDENT_ID = 2L;
    private static final Long COURSE_ID = 1L;

    @Mock private StudentMemoryMapper memoryMapper;
    @Mock private StudentProfileMapper profileMapper;
    @Mock private StrategyRecordMapper strategyMapper;
    @Mock private InterventionRecordMapper interventionMapper;
    @Mock private StudentGoalMapper goalMapper;
    @Mock private AlertRecordMapper alertRecordMapper;
    @Mock private StudyPlanMapper studyPlanMapper;

    private StudentMemoryService service;

    @BeforeEach
    void setUp() {
        service = new StudentMemoryService(memoryMapper, profileMapper, strategyMapper, interventionMapper,
                goalMapper, alertRecordMapper, studyPlanMapper);
    }

    private void stubEmpty() {
        when(profileMapper.selectList(any())).thenReturn(List.of());
        when(strategyMapper.selectList(any())).thenReturn(List.of());
        when(interventionMapper.selectList(any())).thenReturn(List.of());
        when(goalMapper.selectList(any())).thenReturn(List.of());
        when(alertRecordMapper.selectList(any())).thenReturn(List.of());
        when(studyPlanMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("压缩文本包含画像等级、上期策略、目标、预警、计划等关键信息")
    void buildSummaryShouldContainKeyFacts() {
        StudentProfile profile = new StudentProfile();
        profile.setStudentId(STUDENT_ID);
        profile.setCourseId(COURSE_ID);
        profile.setKnowledgeProfile("{\"level\":\"薄弱\"}");
        profile.setHabitProfile("{\"level\":\"放弃型\"}");
        profile.setGoalProfile("{\"level\":\"保底\"}");
        profile.setUpdateTime(LocalDateTime.now());
        when(profileMapper.selectList(any())).thenReturn(List.of(profile));

        StrategyRecord strategy = new StrategyRecord();
        strategy.setStudentId(STUDENT_ID);
        strategy.setCourseId(COURSE_ID);
        strategy.setNewStrategy("降低难度，先补基础题");
        strategy.setChangeReason("连续两次作业未完成");
        strategy.setCreateTime(LocalDateTime.now());
        when(strategyMapper.selectList(any())).thenReturn(List.of(strategy));

        InterventionRecord intervention = new InterventionRecord();
        intervention.setStudentId(STUDENT_ID);
        intervention.setInterventionType("谈话");
        intervention.setDescription("已与辅导员沟通，安排每周答疑");
        intervention.setCreateTime(LocalDateTime.now());
        when(interventionMapper.selectList(any())).thenReturn(List.of(intervention));

        StudentGoal goal = new StudentGoal();
        goal.setStudentId(STUDENT_ID);
        goal.setCourseId(COURSE_ID);
        goal.setGoalLevel("保底");
        goal.setTargetScore(60);
        goal.setUpdateTime(LocalDateTime.now());
        when(goalMapper.selectList(any())).thenReturn(List.of(goal));

        AlertRecord alert = new AlertRecord();
        alert.setStudentId(STUDENT_ID);
        alert.setCourseId(COURSE_ID);
        alert.setAlertLevel("ORANGE");
        alert.setRiskScore(new BigDecimal("72.5"));
        alert.setStatus("ACTIVE");
        alert.setCreateTime(LocalDateTime.now());
        when(alertRecordMapper.selectList(any())).thenReturn(List.of(alert));

        StudyPlan plan = new StudyPlan();
        plan.setId(99L);
        plan.setStudentId(STUDENT_ID);
        plan.setCourseId(COURSE_ID);
        plan.setStatus("ACTIVE");
        plan.setCreateTime(LocalDateTime.now());
        when(studyPlanMapper.selectList(any())).thenReturn(List.of(plan));

        String summary = service.buildSummary(STUDENT_ID, COURSE_ID, 3);

        assertTrue(summary.contains("v3"), summary);
        assertTrue(summary.contains("薄弱"), summary);
        assertTrue(summary.contains("放弃型"), summary);
        assertTrue(summary.contains("降低难度，先补基础题"), summary);
        assertTrue(summary.contains("连续两次作业未完成"), summary);
        assertTrue(summary.contains("谈话"), summary);
        assertTrue(summary.contains("目标分 60"), summary);
        assertTrue(summary.contains("ORANGE"), summary);
        assertTrue(summary.contains("72.5"), summary);
        assertTrue(summary.contains("planId=99"), summary);
    }

    @Test
    @DisplayName("缺数据的维度直接省略，不出现 null 字样")
    void buildSummaryShouldOmitMissingSections() {
        stubEmpty();

        String summary = service.buildSummary(STUDENT_ID, COURSE_ID, 1);

        assertNotNull(summary);
        assertTrue(summary.contains("历史记忆 v1"), summary);
        assertTrue(!summary.contains("null"), summary);
        assertTrue(!summary.contains("学情画像"), "无画像时不应输出该行：" + summary);
    }

    @Test
    @DisplayName("超长文本按上限裁剪，避免撑爆 prompt")
    void buildSummaryShouldTrimLongText() {
        stubEmpty();
        StrategyRecord strategy = new StrategyRecord();
        strategy.setStudentId(STUDENT_ID);
        strategy.setNewStrategy("策".repeat(300));
        strategy.setCreateTime(LocalDateTime.now());
        when(strategyMapper.selectList(any())).thenReturn(List.of(strategy));

        String summary = service.buildSummary(STUDENT_ID, COURSE_ID, 1);

        assertTrue(summary.contains("…"), "超长策略应被裁剪");
        assertTrue(summary.length() < 220, "裁剪后长度应受控：" + summary.length());
    }

    @Test
    @DisplayName("首次刷新写入 v1，第二次刷新 v2 并带上来源运行ID")
    void refreshMemoryShouldIncrementVersion() {
        stubEmpty();
        when(memoryMapper.selectOne(any())).thenReturn(null);
        when(memoryMapper.insert(any(StudentMemory.class))).thenReturn(1);

        StudentMemory first = service.refreshMemory(STUDENT_ID, COURSE_ID, 8L);

        ArgumentCaptor<StudentMemory> captor = ArgumentCaptor.forClass(StudentMemory.class);
        verify(memoryMapper).insert(captor.capture());
        assertEquals(1, first.getVersion());
        assertEquals(8L, captor.getValue().getSourceRunId());
        assertEquals(STUDENT_ID, captor.getValue().getStudentId());
        assertEquals(COURSE_ID, captor.getValue().getCourseId());

        StudentMemory previous = new StudentMemory();
        previous.setVersion(1);
        when(memoryMapper.selectOne(any())).thenReturn(previous);

        StudentMemory second = service.refreshMemory(STUDENT_ID, COURSE_ID, null);

        assertEquals(2, second.getVersion());
        assertNull(second.getSourceRunId());
    }

    @Test
    @DisplayName("没有历史记忆时不注入任何文本")
    void getMemoryTextShouldReturnNullWhenNoMemory() {
        when(memoryMapper.selectOne(any())).thenReturn(null);

        assertNull(service.getMemoryText(STUDENT_ID, COURSE_ID));
        assertNull(service.getMemoryText(null, COURSE_ID));
    }
}
