package com.example.academic_risk_warning.controller.agent;

import com.example.academic_risk_warning.agent.AgentOrchestrator;
import com.example.academic_risk_warning.agent.IntentRouter;
import com.example.academic_risk_warning.agent.exec.ToolCallExecutor;
import com.example.academic_risk_warning.agent.tool.ToolContext;
import com.example.academic_risk_warning.agent.tool.ToolRegistry;
import com.example.academic_risk_warning.agent.tool.ToolResult;
import com.example.academic_risk_warning.agent.tool.ToolSpecLoader;
import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P2-1/2/3 Agent 入口：工具规格、意图列表、编排运行、受控工具调用/JSON 执行（测试用）。
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@RequireRole({"ADMIN", "COUNSELOR"})
public class AgentController {

    private final ToolSpecLoader specLoader;
    private final ToolRegistry registry;
    private final AgentOrchestrator orchestrator;
    private final ToolCallExecutor executor;
    private final ObjectMapper json = new ObjectMapper();

    /** 工具清单；format=openapi 时返回 OpenAPI 3 文档。 */
    @GetMapping("/tools")
    public Result<Object> tools(@RequestParam(required = false, defaultValue = "list") String format) {
        if ("openapi".equalsIgnoreCase(format)) {
            return Result.ok(specLoader.openApi());
        }
        return Result.ok(registry.specSummaries());
    }

    @GetMapping("/intents")
    public Result<List<Map<String, Object>>> intents() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (IntentRouter.Intent i : IntentRouter.Intent.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", i.name());
            m.put("label", i.label());
            out.add(m);
        }
        return Result.ok(out);
    }

    /** 编排运行：路由 → 工具链 → 草稿答案（不含 LLM 生成）。 */
    @PostMapping("/run")
    public Result<AgentOrchestrator.RunResult> run(@RequestBody Map<String, Object> body) {
        String question = body.get("question") == null ? "" : String.valueOf(body.get("question")).trim();
        if (question.isEmpty()) {
            throw new BizException(400, "question 不能为空");
        }
        Integer maxSteps = body.get("maxSteps") == null ? null : ((Number) body.get("maxSteps")).intValue();
        Long termId = body.get("termId") == null ? null : ((Number) body.get("termId")).longValue();
        LoginUser user = UserContext.require();
        return Result.ok(orchestrator.run(question, new ToolContext(user, termId), maxSteps));
    }

    /** 直接执行一个工具（测试/调试，仅教务）。 */
    @PostMapping("/tool-call")
    @RequireRole("ADMIN")
    public Result<ToolResult> toolCall(@RequestBody Map<String, Object> body) {
        String tool = body.get("tool") == null ? "" : String.valueOf(body.get("tool"));
        if (tool.isEmpty()) {
            throw new BizException(400, "tool 不能为空");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> args = body.get("args") instanceof Map ? (Map<String, Object>) body.get("args") : Map.of();
        return Result.ok(registry.execute(tool, args, new ToolContext(UserContext.require(), null)));
    }

    /** 受限 JSON 执行（四道检查，仅教务；用于 P2-3 验收）。 */
    @PostMapping("/execute-json")
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> executeJson(@RequestBody Map<String, Object> body) {
        String raw = body.get("json") == null ? "" : String.valueOf(body.get("json"));
        int maxSteps = body.get("maxSteps") == null ? 1 : ((Number) body.get("maxSteps")).intValue();
        int usedSteps = body.get("usedSteps") == null ? 0 : ((Number) body.get("usedSteps")).intValue();
        ToolCallExecutor.Outcome out = executor.execute(raw, new ToolContext(UserContext.require(), null), usedSteps, maxSteps);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ok", out.ok());
        data.put("tool", out.tool());
        data.put("args", out.args());
        data.put("checks", out.checks());
        data.put("usedStepsAfter", out.usedStepsAfter());
        data.put("result", out.result());
        return Result.ok(data);
    }
}
