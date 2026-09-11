package com.example.academic_risk_warning.agent.recommend;

import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.core.AgentResult;
import com.example.academic_risk_warning.agent.core.AgentRunContext;
import com.example.academic_risk_warning.config.AgentProperties;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.StudyPlan;
import com.example.academic_risk_warning.llm.LLMClient;
import com.example.academic_risk_warning.mapper.AlertRecordMapper;
import com.example.academic_risk_warning.mapper.CourseKnowledgePointMapper;
import com.example.academic_risk_warning.mapper.ExerciseKnowledgePointMapper;
import com.example.academic_risk_warning.mapper.ExerciseMapper;
import com.example.academic_risk_warning.mapper.ExerciseRecommendationMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.StudentWeakPointMapper;
import com.example.academic_risk_warning.mapper.StudyPlanMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * W2 反思环（Self-Refine）测试：初稿校验不通过时带着批评重写一版，
 * 并把轮次与批评写进运行记录；关闭开关时只调用一次。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("推荐智能体反思环测试")
class RecommendAgentReflectionTest {

    private static final Long STUDENT_ID = 2L;
    private static final Long COURSE_ID = 1L;

    @Mock private LLMClient llmClient;
    @Mock private StudyPlanMapper studyPlanMapper;
    @Mock private StudentWeakPointMapper weakPointMapper;
    @Mock private ExerciseMapper exerciseMapper;
    @Mock private ExerciseKnowledgePointMapper exerciseKpMapper;
    @Mock private ExerciseRecommendationMapper recommendMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private CourseKnowledgePointMapper kpMapper;
    @Mock private AlertRecordMapper alertRecordMapper;
    @Mock private com.example.academic_risk_warning.service.StudentMemoryService studentMemoryService;

    private RecommendAgent agent(AgentProperties properties) {
        return new RecommendAgent(llmClient, studyPlanMapper, weakPointMapper, exerciseMapper, exerciseKpMapper,
                recommendMapper, studentMapper, kpMapper, alertRecordMapper, properties, studentMemoryService);
    }

    private void stubCommon() {
        Student student = new Student();
        student.setId(STUDENT_ID);
        student.setStudentName("测试学生");
        student.setGrade("大一上");
        lenient().when(studentMapper.selectById(any())).thenReturn(student);
        lenient().when(weakPointMapper.selectList(any())).thenReturn(List.of());
        lenient().when(exerciseMapper.selectList(any())).thenReturn(List.of());
        lenient().when(recommendMapper.selectList(any())).thenReturn(List.of());
        lenient().when(alertRecordMapper.selectOne(any())).thenReturn(null);
        lenient().when(studyPlanMapper.insert(any(StudyPlan.class))).thenReturn(1);
    }

    @Test
    @DisplayName("初稿缺 plan → 触发反思重写，记录 attempts=2 与批评内容")
    void shouldRefineWhenValidationFails() {
        stubCommon();
        AgentProperties properties = new AgentProperties(); // 默认开启反思
        // 第一次：只有总结，缺 plan/推荐 → 校验 FAIL；第二次：合规
        when(llmClient.chat(anyString(), anyString()))
                .thenReturn("{\"summary\":\"只有一句总结\"}")
                .thenReturn("{\"plan\":{\"stages\":[{\"name\":\"第1周\"}]},\"recommendations\":[]}");

        AgentContext ctx = new AgentContext(STUDENT_ID);
        ctx.setCourseId(COURSE_ID);

        AgentRunContext.begin();
        AgentResult<Map<String, Object>> result = agent(properties).execute(ctx);
        List<AgentRunContext.StepRecord> steps = AgentRunContext.collectAndClear();

        assertTrue(result.isSuccess(), result.getErrorMessage());
        verify(llmClient, times(2)).chat(anyString(), anyString());

        assertEquals(1, steps.size());
        AgentRunContext.StepRecord step = steps.get(0);
        assertEquals(2, step.attempts(), "应记录两次 LLM 调用");
        assertNotNull(step.reflection(), "应记录反思批评");
        assertTrue(step.reflection().contains("plan"), step.reflection());
        assertEquals("PASS", step.validationStatus(), "重写后应通过校验");
        assertTrue(result.getData().containsKey("plan"));
    }

    @Test
    @DisplayName("关闭反思开关 → 只调用一次 LLM，attempts=1")
    void shouldNotRefineWhenDisabled() {
        stubCommon();
        AgentProperties properties = new AgentProperties();
        properties.setReflectionEnabled(false);
        when(llmClient.chat(anyString(), anyString()))
                .thenReturn("{\"plan\":{\"stages\":[{\"name\":\"第1周\"}]}}");

        AgentContext ctx = new AgentContext(STUDENT_ID);
        ctx.setCourseId(COURSE_ID);

        AgentRunContext.begin();
        agent(properties).execute(ctx);
        List<AgentRunContext.StepRecord> steps = AgentRunContext.collectAndClear();

        verify(llmClient, times(1)).chat(anyString(), anyString());
        assertEquals(1, steps.get(0).attempts());
    }

    @Test
    @DisplayName("初稿已合规 → 不触发重写")
    void shouldNotRefineWhenAlreadyValid() {
        stubCommon();
        AgentProperties properties = new AgentProperties();
        when(llmClient.chat(anyString(), anyString()))
                .thenReturn("{\"plan\":{\"stages\":[{\"name\":\"第1周\"}]},\"recommendations\":[]}");

        AgentContext ctx = new AgentContext(STUDENT_ID);
        ctx.setCourseId(COURSE_ID);

        AgentRunContext.begin();
        agent(properties).execute(ctx);
        List<AgentRunContext.StepRecord> steps = AgentRunContext.collectAndClear();

        verify(llmClient, times(1)).chat(anyString(), anyString());
        assertEquals("PASS", steps.get(0).validationStatus());
        assertEquals(1, steps.get(0).attempts());
    }
}
