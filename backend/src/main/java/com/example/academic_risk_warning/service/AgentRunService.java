package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.agent.core.AgentRunContext;
import com.example.academic_risk_warning.entity.AgentRun;
import com.example.academic_risk_warning.entity.AgentRunStep;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.mapper.AgentRunMapper;
import com.example.academic_risk_warning.mapper.AgentRunStepMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能体运行记录服务（W1：可观测 / 可复现）。
 *
 * <p>写：编排器每跑完一条流水线，把 {@link AgentRunContext} 收集到的步骤交进来落库；
 * 读：教师端"运行历史"面板与 {@code /api/agent/stats} 的真实统计。
 */
@Service
public class AgentRunService {

    private static final int STATS_RUN_LIMIT = 200;
    private static final int STATS_STEP_LIMIT = 1000;

    private final AgentRunMapper agentRunMapper;
    private final AgentRunStepMapper agentRunStepMapper;
    private final StudentMapper studentMapper;

    public AgentRunService(AgentRunMapper agentRunMapper,
                           AgentRunStepMapper agentRunStepMapper,
                           StudentMapper studentMapper) {
        this.agentRunMapper = agentRunMapper;
        this.agentRunStepMapper = agentRunStepMapper;
        this.studentMapper = studentMapper;
    }

    /**
     * 保存一次运行：主记录 + 每步明细。
     *
     * @return runId
     */
    @Transactional
    public Long saveRun(Long studentId, Long courseId, String pipeline, String triggerType,
                        List<AgentRunContext.StepRecord> steps) {
        List<AgentRunContext.StepRecord> safeSteps = steps == null ? List.of() : steps;

        int succeeded = (int) safeSteps.stream().filter(s -> "SUCCESS".equals(s.status())).count();
        int failed = safeSteps.size() - succeeded;
        long totalMs = safeSteps.stream().mapToLong(AgentRunContext.StepRecord::durationMs).sum();

        AgentRun run = new AgentRun();
        run.setStudentId(studentId);
        run.setCourseId(courseId);
        run.setPipeline(pipeline);
        run.setTriggerType(triggerType);
        run.setTotalSteps(safeSteps.size());
        run.setSucceededSteps(succeeded);
        run.setFailedSteps(failed);
        run.setTotalMs(totalMs);
        run.setStatus(safeSteps.isEmpty() ? "RUNNING"
                : failed == 0 ? "SUCCESS"
                : succeeded == 0 ? "FAILED" : "PARTIAL");
        run.setError(truncate(firstError(safeSteps)));
        run.setCreateTime(LocalDateTime.now());
        run.setFinishTime(LocalDateTime.now());
        agentRunMapper.insert(run);

        int stepNo = 1;
        for (AgentRunContext.StepRecord s : safeSteps) {
            AgentRunStep step = new AgentRunStep();
            step.setRunId(run.getId());
            step.setStepNo(stepNo++);
            step.setAgentName(s.agentName());
            step.setStatus(s.status());
            step.setDurationMs(s.durationMs());
            step.setOutputDigest(s.outputDigest());
            step.setError(s.error());
            step.setValidationStatus(s.validationStatus());
            step.setValidationDetail(s.validationDetail());
            step.setReflection(s.reflection());
            step.setAttempts(s.attempts());
            step.setOutputFields(s.outputFields());
            step.setCreateTime(LocalDateTime.now());
            agentRunStepMapper.insert(step);
        }
        return run.getId();
    }

    /** 某学生（或全部）的运行历史，含各步明细 */
    public List<Map<String, Object>> listRuns(Long studentId, int limit) {
        int size = limit <= 0 ? 10 : Math.min(limit, 50);
        List<AgentRun> runs = agentRunMapper.selectList(new LambdaQueryWrapper<AgentRun>()
                .eq(studentId != null, AgentRun::getStudentId, studentId)
                .orderByDesc(AgentRun::getCreateTime)
                .last("LIMIT " + size));
        if (runs.isEmpty()) {
            return List.of();
        }
        Map<Long, List<AgentRunStep>> stepsByRun = loadStepsByRun(
                runs.stream().map(AgentRun::getId).toList());

        Map<Long, String> studentNames = loadStudentNames(
                runs.stream().map(AgentRun::getStudentId).collect(Collectors.toSet()));

        return runs.stream().map(run -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("runId", run.getId());
            m.put("studentId", run.getStudentId());
            m.put("studentName", studentNames.get(run.getStudentId()));
            m.put("courseId", run.getCourseId());
            m.put("pipeline", run.getPipeline());
            m.put("triggerType", run.getTriggerType());
            m.put("status", run.getStatus());
            m.put("totalSteps", run.getTotalSteps());
            m.put("succeededSteps", run.getSucceededSteps());
            m.put("failedSteps", run.getFailedSteps());
            m.put("totalMs", run.getTotalMs());
            m.put("error", run.getError());
            m.put("createTime", run.getCreateTime());
            m.put("steps", stepsByRun.getOrDefault(run.getId(), List.of()).stream()
                    .map(this::toStepMap).toList());
            return m;
        }).toList();
    }

    /** 单次运行明细 */
    public Map<String, Object> getRunDetail(Long runId) {
        AgentRun run = agentRunMapper.selectById(runId);
        if (run == null) {
            return Map.of("exists", false);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("exists", true);
        m.put("runId", run.getId());
        m.put("studentId", run.getStudentId());
        m.put("courseId", run.getCourseId());
        m.put("pipeline", run.getPipeline());
        m.put("status", run.getStatus());
        m.put("totalSteps", run.getTotalSteps());
        m.put("succeededSteps", run.getSucceededSteps());
        m.put("failedSteps", run.getFailedSteps());
        m.put("totalMs", run.getTotalMs());
        m.put("error", run.getError());
        m.put("createTime", run.getCreateTime());
        m.put("steps", loadStepsByRun(List.of(runId)).getOrDefault(runId, List.of()).stream()
                .map(this::toStepMap).toList());
        return m;
    }

    /**
     * 真实运行统计：总次数、成功率、平均/P95 耗时、各智能体失败率与平均耗时、最近运行。
     */
    public Map<String, Object> getStats(int recentLimit) {
        List<AgentRun> runs = agentRunMapper.selectList(new LambdaQueryWrapper<AgentRun>()
                .orderByDesc(AgentRun::getCreateTime)
                .last("LIMIT " + STATS_RUN_LIMIT));
        List<AgentRunStep> steps = agentRunStepMapper.selectList(new LambdaQueryWrapper<AgentRunStep>()
                .orderByDesc(AgentRunStep::getId)
                .last("LIMIT " + STATS_STEP_LIMIT));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRuns", runs.size());
        stats.put("successRuns", runs.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count());
        stats.put("partialRuns", runs.stream().filter(r -> "PARTIAL".equals(r.getStatus())).count());
        stats.put("failedRuns", runs.stream().filter(r -> "FAILED".equals(r.getStatus())).count());
        double successRate = runs.isEmpty() ? 0
                : Math.round(runs.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count() * 1000.0 / runs.size()) / 10.0;
        stats.put("successRate", successRate);

        List<Long> durations = runs.stream().map(AgentRun::getTotalMs).filter(Objects::nonNull).sorted().toList();
        stats.put("avgTotalMs", durations.isEmpty() ? 0 : Math.round(durations.stream().mapToLong(Long::longValue).average().orElse(0)));
        stats.put("p95TotalMs", percentile(durations, 0.95));

        // W2：输出校验通过率 / 反思重写次数
        long validated = steps.stream().filter(this::isValidated).count();
        long passedSteps = steps.stream().filter(s -> "PASS".equals(s.getValidationStatus())).count();
        stats.put("validatedSteps", validated);
        stats.put("schemaPassRate", validated == 0 ? 0
                : Math.round(passedSteps * 1000.0 / validated) / 10.0);
        stats.put("reflectedSteps", steps.stream()
                .filter(s -> s.getAttempts() != null && s.getAttempts() > 1).count());

        // W3 消融视角：需要反思重写的步骤 vs 一次通过的步骤
        Map<String, Object> ablation = new LinkedHashMap<>();
        ablation.put("reflected", groupStats(steps.stream()
                .filter(s -> s.getAttempts() != null && s.getAttempts() > 1).toList()));
        ablation.put("direct", groupStats(steps.stream()
                .filter(s -> s.getAttempts() == null || s.getAttempts() <= 1).toList()));
        stats.put("ablation", ablation);

        // 各智能体维度
        Map<String, List<AgentRunStep>> byAgent = steps.stream()
                .collect(Collectors.groupingBy(AgentRunStep::getAgentName, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> agents = new ArrayList<>();
        byAgent.forEach((agent, list) -> {
            long failedCount = list.stream().filter(s -> !"SUCCESS".equals(s.getStatus())).count();
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("agent", agent);
            a.put("steps", (long) list.size());
            a.put("failed", failedCount);
            a.put("failureRate", list.isEmpty() ? 0
                    : Math.round(failedCount * 1000.0 / list.size()) / 10.0);
            a.put("avgMs", Math.round(list.stream()
                    .map(AgentRunStep::getDurationMs).filter(Objects::nonNull)
                    .mapToLong(Long::longValue).average().orElse(0)));
            long validatedN = list.stream().filter(this::isValidated).count();
            long passedN = list.stream().filter(s -> "PASS".equals(s.getValidationStatus())).count();
            a.put("validated", validatedN);
            a.put("passRate", validatedN == 0 ? 0 : Math.round(passedN * 1000.0 / validatedN) / 10.0);
            a.put("reflected", list.stream().filter(s -> s.getAttempts() != null && s.getAttempts() > 1).count());
            agents.add(a);
        });
        agents.sort((x, y) -> Long.compare((Long) y.get("steps"), (Long) x.get("steps")));
        stats.put("agents", agents);

        int size = recentLimit <= 0 ? 5 : Math.min(recentLimit, 20);
        stats.put("recentRuns", listRuns(null, size));
        return stats;
    }

    // ==================== 内部工具 ====================

    /** 取首个错误信息并截断，保证不会超过 error 列宽（varchar(1000)） */
    private String firstError(List<AgentRunContext.StepRecord> steps) {
        return truncate(steps.stream()
                .map(AgentRunContext.StepRecord::error)
                .filter(Objects::nonNull)
                .findFirst().orElse(null));
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= 500 ? text : text.substring(0, 499) + "…";
    }

    /** 该步是否有有效校验结论（SKIP/空表示没有跑校验，不计入通过率） */
    private boolean isValidated(AgentRunStep step) {
        String status = step.getValidationStatus();
        return status != null && !status.isBlank() && !"SKIP".equals(status);
    }

    /** 一组步骤的规模/校验通过率/平均耗时（消融对比用） */
    private Map<String, Object> groupStats(List<AgentRunStep> list) {
        Map<String, Object> m = new LinkedHashMap<>();
        long validated = list.stream().filter(this::isValidated).count();
        long passed = list.stream().filter(s -> "PASS".equals(s.getValidationStatus())).count();
        m.put("steps", list.size());
        m.put("validated", validated);
        m.put("passRate", validated == 0 ? 0 : Math.round(passed * 1000.0 / validated) / 10.0);
        m.put("avgMs", Math.round(list.stream().map(AgentRunStep::getDurationMs)
                .filter(Objects::nonNull).mapToLong(Long::longValue).average().orElse(0)));
        return m;
    }

    private Map<Long, List<AgentRunStep>> loadStepsByRun(List<Long> runIds) {
        if (runIds.isEmpty()) {
            return Map.of();
        }
        List<AgentRunStep> steps = agentRunStepMapper.selectList(new LambdaQueryWrapper<AgentRunStep>()
                .in(AgentRunStep::getRunId, runIds)
                .orderByAsc(AgentRunStep::getRunId)
                .orderByAsc(AgentRunStep::getStepNo));
        return steps.stream().collect(Collectors.groupingBy(AgentRunStep::getRunId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<Long, String> loadStudentNames(Set<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        return studentMapper.selectList(new LambdaQueryWrapper<Student>().in(Student::getId, studentIds))
                .stream().collect(Collectors.toMap(Student::getId, Student::getStudentName, (a, b) -> a));
    }

    private Map<String, Object> toStepMap(AgentRunStep step) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stepNo", step.getStepNo());
        m.put("agent", step.getAgentName());
        m.put("status", step.getStatus());
        m.put("durationMs", step.getDurationMs());
        m.put("error", step.getError());
        m.put("outputDigest", step.getOutputDigest());
        m.put("validationStatus", step.getValidationStatus());
        m.put("validationDetail", step.getValidationDetail());
        m.put("reflection", step.getReflection());
        m.put("attempts", step.getAttempts());
        m.put("outputFields", step.getOutputFields());
        return m;
    }

    /** 简单百分位（升序列表，取 ceil(p*n)-1 位置） */
    private long percentile(List<Long> sortedAsc, double p) {
        if (sortedAsc.isEmpty()) {
            return 0;
        }
        int idx = (int) Math.ceil(p * sortedAsc.size()) - 1;
        return sortedAsc.get(Math.max(0, Math.min(idx, sortedAsc.size() - 1)));
    }
}
