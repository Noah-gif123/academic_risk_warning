package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.agent.core.JsonUtils;
import com.example.academic_risk_warning.config.AgentProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Golden set 离线评测（W2）。
 *
 * <p>用例来自 {@code resources/golden/agent_golden_cases.json}，每个用例声明：
 * 目标智能体、输入（学生/课程）、期望产出字段。判定标准：
 * <ol>
 *   <li>该步执行成功；</li>
 *   <li>输出校验不是 FAIL（结构/取值/真值一致性）；</li>
 *   <li>期望字段都出现在产出摘要里（摘要截断 500 字，属尽力而为的覆盖检查）。</li>
 * </ol>
 *
 * <p>两种模式：{@code live=false} 用已落库的运行结果评测（不花钱）；
 * {@code live=true} 会对每个用例真实跑一次完整评估（消耗大模型额度，用于论文出表）。
 */
@Service
public class AgentEvalService {

    private static final Logger log = LoggerFactory.getLogger(AgentEvalService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String GOLDEN_PATH = "golden/agent_golden_cases.json";

    private final AgentRunService agentRunService;
    private final RiskCenterService riskCenterService;
    private final AgentProperties agentProperties;

    public AgentEvalService(AgentRunService agentRunService, RiskCenterService riskCenterService,
                            AgentProperties agentProperties) {
        this.agentRunService = agentRunService;
        this.riskCenterService = riskCenterService;
        this.agentProperties = agentProperties;
    }

    /**
     * 消融对比：有反思 vs 无反思。
     *
     * <p>live=false：直接按已落库步骤分组统计（"反思过的步骤" vs "一次通过的步骤"）；
     * live=true：对每个 golden 用例分别以"关闭反思"和"开启反思"各真实跑一次并比较。
     * 注意：live 模式会临时切换全局配置，评测期间不要并发跑其它流水线。
     */
    public Map<String, Object> ablation(int limit, boolean live) {
        if (!live) {
            Object stored = agentRunService.getStats(1).get("ablation");
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("mode", "STORED");
            out.put("stored", stored);
            out.put("note", "按历史步骤分组：reflected=触发过反思重写的步骤，direct=一次通过的步骤");
            return out;
        }

        List<Map<String, Object>> cases = new ArrayList<>();
        for (GoldenCase golden : loadCases()) {
            Map<String, Object> off = runWithReflection(golden, false);
            Map<String, Object> on = runWithReflection(golden, true);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("caseId", golden.caseId());
            item.put("agent", golden.agent());
            item.put("withoutReflection", off);
            item.put("withReflection", on);
            cases.add(item);
        }
        long onPass = cases.stream().filter(c -> passed(c.get("withReflection"))).count();
        long offPass = cases.stream().filter(c -> passed(c.get("withoutReflection"))).count();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "LIVE");
        out.put("total", cases.size());
        out.put("passWithoutReflection", offPass);
        out.put("passWithReflection", onPass);
        out.put("cases", cases);
        out.put("note", "同一批 golden 用例分别关闭/开启反思各跑一次；差异即 Self-Refine 的增益");
        return out;
    }

    private boolean passed(Object judged) {
        return judged instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get("pass"));
    }

    private Map<String, Object> runWithReflection(GoldenCase golden, boolean enabled) {
        boolean original = agentProperties.isReflectionEnabled();
        agentProperties.setReflectionEnabled(enabled);
        try {
            return evaluateLive(golden);
        } finally {
            agentProperties.setReflectionEnabled(original);
        }
    }

    /** golden 用例 */
    public record GoldenCase(String caseId, String agent, Long studentId, Long courseId,
                             List<String> expectedFields, String note) {}

    public Map<String, Object> evaluate(int limit, boolean live) {
        List<GoldenCase> cases = loadCases();
        List<Map<String, Object>> results = new ArrayList<>();
        int passed = 0;
        for (GoldenCase c : cases) {
            Map<String, Object> r = live ? evaluateLive(c) : evaluateFromStored(c, limit);
            results.add(r);
            if (Boolean.TRUE.equals(r.get("pass"))) {
                passed++;
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mode", live ? "LIVE" : "STORED");
        out.put("total", results.size());
        out.put("passed", passed);
        out.put("failed", results.size() - passed);
        out.put("passRate", results.isEmpty() ? 0 : Math.round(passed * 1000.0 / results.size()) / 10.0);
        out.put("cases", results);
        return out;
    }

    // ==================== 两种评测模式 ====================

    private Map<String, Object> evaluateFromStored(GoldenCase golden, int limit) {
        List<Map<String, Object>> runs = agentRunService.listRuns(golden.studentId(), limit);
        Map<String, Object> step = findStep(runs, golden.agent());
        return judge(golden, step, "STORED");
    }

    private Map<String, Object> evaluateLive(GoldenCase golden) {
        try {
            Map<String, Object> run = riskCenterService.runFullAssessment(golden.studentId(), golden.courseId());
            Long runId = extractRunId(run);
            Map<String, Object> step = runId == null
                    ? null
                    : findStepInDetail(agentRunService.getRunDetail(runId), golden.agent());
            Map<String, Object> judged = judge(golden, step, "LIVE");
            judged.put("pipelineMs", run.get("durationMs"));
            return judged;
        } catch (Exception e) {
            log.error("[AgentEval] 用例 {} 实跑失败", golden.caseId(), e);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("caseId", golden.caseId());
            r.put("agent", golden.agent());
            r.put("mode", "LIVE");
            r.put("pass", false);
            r.put("reason", "实跑失败: " + e.getMessage());
            return r;
        }
    }

    // ==================== 判定与工具 ====================

    private Map<String, Object> judge(GoldenCase golden, Map<String, Object> step, String mode) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("caseId", golden.caseId());
        r.put("agent", golden.agent());
        r.put("mode", mode);
        r.put("expectedFields", golden.expectedFields());
        r.put("note", golden.note());

        if (step == null) {
            r.put("pass", false);
            r.put("reason", "没有找到该智能体的运行记录（请先跑一次完整评估）");
            return r;
        }

        String status = str(step.get("status"));
        String validation = str(step.get("validationStatus"));
        String digest = str(step.get("outputDigest"));
        String fields = str(step.get("outputFields"));
        List<String> missing = golden.expectedFields().stream()
                .filter(f -> !fieldPresent(fields, digest, f))
                .toList();

        boolean ok = "SUCCESS".equals(status) && !"FAIL".equals(validation) && missing.isEmpty();
        r.put("pass", ok);
        r.put("stepStatus", status);
        r.put("validationStatus", validation);
        r.put("validationDetail", step.get("validationDetail"));
        r.put("missingFields", missing);
        r.put("attempts", step.get("attempts"));
        r.put("stepMs", step.get("durationMs"));
        if (!ok) {
            r.put("reason", !"SUCCESS".equals(status) ? "该步执行失败"
                    : (!missing.isEmpty() ? "产出摘要缺少字段: " + missing : "输出校验未通过: " + validation));
        }
        return r;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findStep(List<Map<String, Object>> runs, String agent) {
        for (Map<String, Object> run : runs) {
            Object steps = run.get("steps");
            if (steps instanceof List<?> list) {
                for (Object s : list) {
                    if (s instanceof Map<?, ?> map && agent.equals(map.get("agent"))) {
                        return (Map<String, Object>) map;
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findStepInDetail(Map<String, Object> detail, String agent) {
        if (detail == null) {
            return null;
        }
        Object steps = detail.get("steps");
        if (steps instanceof List<?> list) {
            for (Object s : list) {
                if (s instanceof Map<?, ?> map && agent.equals(map.get("agent"))) {
                    return (Map<String, Object>) map;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Long extractRunId(Map<String, Object> run) {
        if (run == null) {
            return null;
        }
        Object pipeline = run.get("pipelineResult");
        if (pipeline instanceof Map<?, ?> map) {
            Object runId = map.get("runId");
            if (runId instanceof Number n) {
                return n.longValue();
            }
        }
        Object direct = run.get("runId");
        return direct instanceof Number n ? n.longValue() : null;
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 期望字段是否出现：优先用产出字段清单（精确，且不受摘要截断影响）；
     * 老记录没有该列时退回"摘要包含"判断。
     */
    private boolean fieldPresent(String fields, String digest, String expected) {
        if (!fields.isBlank()) {
            for (String f : fields.split(",")) {
                if (f.trim().equals(expected)) {
                    return true;
                }
            }
            return false;
        }
        return digest.contains(expected);
    }

    private List<GoldenCase> loadCases() {
        try (InputStream in = new ClassPathResource(GOLDEN_PATH).getInputStream()) {
            Map<String, Object> doc = MAPPER.readValue(in, new TypeReference<>() {});
            Object cases = doc.get("cases");
            if (!(cases instanceof List<?> list)) {
                return List.of();
            }
            List<GoldenCase> result = new ArrayList<>();
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                result.add(new GoldenCase(
                        str(map.get("caseId")),
                        str(map.get("agent")),
                        asLong(map.get("studentId")),
                        asLong(map.get("courseId")),
                        asStringList(map.get("expectedFields")),
                        str(map.get("note"))));
            }
            return result;
        } catch (Exception e) {
            log.warn("[AgentEval] golden 用例加载失败: {}", e.getMessage());
            return List.of();
        }
    }

    private Long asLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return value == null ? null : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(v -> JsonUtils.toJsonString(v).replace("\"", "")).toList();
    }
}
