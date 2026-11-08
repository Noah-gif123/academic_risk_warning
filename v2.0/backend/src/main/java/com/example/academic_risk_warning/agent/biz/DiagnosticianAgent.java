package com.example.academic_risk_warning.agent.biz;

import com.example.academic_risk_warning.agent.AgentOrchestrator;
import com.example.academic_risk_warning.agent.tool.ToolContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P2-7 Diagnostician：研判契约。
 *
 * <p>输入问题 → 路由/计划/工具执行 → 输出结构化研判：
 * {answer, intent, tools, evidence, ledger, refused, latencyMs}。
 * 工具选择走 P2-3 编排器（规则 + 受限 JSON 执行），后续可替换为 LLM 选路。</p>
 */
@Component
@RequiredArgsConstructor
public class DiagnosticianAgent {

    private final AgentOrchestrator orchestrator;

    public Map<String, Object> diagnose(String question, ToolContext ctx) {
        AgentOrchestrator.RunResult r = orchestrator.run(question, ctx, 4, "auto");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("agent", "diagnostician");
        out.put("question", question);
        out.put("intent", r.intent());
        out.put("intentLabel", r.intentLabel());
        out.put("routeMethod", r.routeMethod());
        List<String> tools = new ArrayList<>();
        List<Map<String, Object>> evidence = new ArrayList<>();
        for (AgentOrchestrator.Step s : r.steps()) {
            tools.add(s.tool());
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("step", s.stepNo());
            e.put("tool", s.tool());
            e.put("evidence", s.evidence());
            evidence.add(e);
        }
        out.put("tools", tools);
        out.put("evidence", evidence);
        out.put("answer", r.answerDraft());
        out.put("refused", r.blocked());
        out.put("blockedReason", r.blockedReason());
        out.put("ledger", r.ledger());
        out.put("quality", r.quality());
        out.put("latencyMs", r.elapsedMs());
        out.put("traceRunId", r.traceRunId());
        return out;
    }
}
