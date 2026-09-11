package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.agent.core.AgentRunContext;
import com.example.academic_risk_warning.entity.AgentRun;
import com.example.academic_risk_warning.entity.AgentRunStep;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.mapper.AgentRunMapper;
import com.example.academic_risk_warning.mapper.AgentRunStepMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 智能体运行记录测试：状态判定、步骤落库、真实统计聚合、运行上下文缓冲。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("智能体运行记录与统计测试")
class AgentRunServiceTest {

    private static final Long STUDENT_ID = 3L;

    @Mock private AgentRunMapper agentRunMapper;
    @Mock private AgentRunStepMapper agentRunStepMapper;
    @Mock private StudentMapper studentMapper;

    private AgentRunService service() {
        return new AgentRunService(agentRunMapper, agentRunStepMapper, studentMapper);
    }

    @Test
    @DisplayName("部分步骤失败 → 状态 PARTIAL，且每一步都落库")
    void saveRunShouldPersistStepsAndMarkPartial() {
        when(agentRunMapper.insert(any(AgentRun.class))).thenAnswer(inv -> {
            AgentRun run = inv.getArgument(0);
            run.setId(99L);
            return 1;
        });

        Long runId = service().saveRun(STUDENT_ID, 1L, "FULL", "MANUAL", List.of(
                step("MonitorAgent", "SUCCESS", 100, "{\"a\":1}", null),
                step("AnalysisAgent", "FAILED", 50, null, "LLM 超时"),
                step("ProfileAgent", "SUCCESS", 200, "{\"b\":2}", null)));

        assertEquals(99L, runId);

        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunMapper).insert(runCaptor.capture());
        AgentRun run = runCaptor.getValue();
        assertEquals("PARTIAL", run.getStatus());
        assertEquals(3, run.getTotalSteps());
        assertEquals(2, run.getSucceededSteps());
        assertEquals(1, run.getFailedSteps());
        assertEquals(350L, run.getTotalMs());
        assertEquals("LLM 超时", run.getError());

        verify(agentRunStepMapper, times(3)).insert(any(AgentRunStep.class));
    }

    @Test
    @DisplayName("全部成功 → SUCCESS；全部失败 → FAILED")
    void saveRunShouldJudgeOverallStatus() {
        when(agentRunMapper.insert(any(AgentRun.class))).thenReturn(1);

        service().saveRun(STUDENT_ID, null, "FULL", "MANUAL", List.of(
                step("MonitorAgent", "SUCCESS", 10, null, null)));
        service().saveRun(STUDENT_ID, null, "FULL", "MANUAL", List.of(
                step("MonitorAgent", "FAILED", 10, null, "err")));

        ArgumentCaptor<AgentRun> captor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunMapper, times(2)).insert(captor.capture());
        assertEquals("SUCCESS", captor.getAllValues().get(0).getStatus());
        assertEquals("FAILED", captor.getAllValues().get(1).getStatus());
    }

    @Test
    @DisplayName("统计聚合：成功率 / 平均与 P95 耗时 / 各智能体失败率")
    void statsShouldAggregateRealNumbers() {
        AgentRun ok = run(1L, "SUCCESS", 100L, 3, 3, 0);
        AgentRun bad = run(2L, "FAILED", 300L, 1, 0, 1);
        when(agentRunMapper.selectList(any())).thenReturn(List.of(bad, ok));

        AgentRunStep s1 = step(1L, 1, "MonitorAgent", "SUCCESS", 40L);
        AgentRunStep s2 = step(1L, 2, "AnalysisAgent", "SUCCESS", 20L);
        AgentRunStep s3 = step(2L, 1, "MonitorAgent", "FAILED", 60L);
        when(agentRunStepMapper.selectList(any())).thenReturn(List.of(s1, s2, s3));

        Student student = new Student();
        student.setId(STUDENT_ID);
        student.setStudentName("测试学生");
        when(studentMapper.selectList(any())).thenReturn(List.of(student));

        Map<String, Object> stats = service().getStats(5);

        assertEquals(2, stats.get("totalRuns"));
        assertEquals(1L, stats.get("successRuns"));
        assertEquals(1L, stats.get("failedRuns"));
        assertEquals(50.0, stats.get("successRate"));
        assertEquals(200L, stats.get("avgTotalMs"));
        assertEquals(300L, stats.get("p95TotalMs"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> agents = (List<Map<String, Object>>) stats.get("agents");
        Map<String, Object> monitor = agents.stream()
                .filter(a -> "MonitorAgent".equals(a.get("agent"))).findFirst().orElseThrow();
        assertEquals(2L, monitor.get("steps"));
        assertEquals(1L, monitor.get("failed"));
        assertEquals(50.0, monitor.get("failureRate"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recent = (List<Map<String, Object>>) stats.get("recentRuns");
        assertEquals(2, recent.size());
        assertTrue(recent.get(0).containsKey("steps"));
    }

    @Test
    @DisplayName("运行上下文：begin → record → collectAndClear")
    void runContextShouldBufferSteps() {
        AgentRunContext.begin();
        assertTrue(AgentRunContext.isActive());

        AgentRunContext.record("MonitorAgent", true, 12, Map.of("k", "v"), null);
        AgentRunContext.record("AnalysisAgent", false, 34, null, "boom");

        List<AgentRunContext.StepRecord> steps = AgentRunContext.collectAndClear();
        assertEquals(2, steps.size());
        assertFalse(AgentRunContext.isActive());
        assertEquals("SUCCESS", steps.get(0).status());
        assertNotNull(steps.get(0).outputDigest());
        assertEquals("FAILED", steps.get(1).status());
        assertEquals("boom", steps.get(1).error());

        // 未 begin 时调用不应报错
        AgentRunContext.record("X", true, 1, null, null);
    }

    private AgentRunContext.StepRecord step(String agent, String status, long ms, String digest, String error) {
        return new AgentRunContext.StepRecord(agent, status, ms, digest, error, "PASS", "OK", null, 1,
                "knowledgeProfile,habitProfile");
    }

    private AgentRun run(Long id, String status, long totalMs, int total, int ok, int failed) {
        AgentRun run = new AgentRun();
        run.setId(id);
        run.setStudentId(STUDENT_ID);
        run.setPipeline("FULL");
        run.setStatus(status);
        run.setTotalMs(totalMs);
        run.setTotalSteps(total);
        run.setSucceededSteps(ok);
        run.setFailedSteps(failed);
        run.setCreateTime(java.time.LocalDateTime.now());
        return run;
    }

    private AgentRunStep step(Long runId, int no, String agent, String status, long ms) {
        AgentRunStep step = new AgentRunStep();
        step.setRunId(runId);
        step.setStepNo(no);
        step.setAgentName(agent);
        step.setStatus(status);
        step.setDurationMs(ms);
        return step;
    }
}
