package com.example.academic_risk_warning.controller.agent;

import com.example.academic_risk_warning.agent.tool.ToolContext;
import com.example.academic_risk_warning.agent.tool.ToolRegistry;
import com.example.academic_risk_warning.agent.tool.ToolResult;
import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.config.AiProperties;
import com.example.academic_risk_warning.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * P3：Dify 自定义工具回调入口。
 *
 * <p>用独立的服务令牌（X-Dify-Token）而不是复用用户 JWT，避免 Dify 工具凭据泄露后
 * 获得任意用户权限；仍然走同一个 ToolRegistry 的白名单/参数/超时校验。</p>
 */
@RestController
@RequestMapping("/api/agent/dify/tool")
@RequiredArgsConstructor
public class DifyToolController {

    private final ToolRegistry registry;
    private final AiProperties props;

    @PostMapping("/{tool}")
    public Result<Map<String, Object>> call(@PathVariable String tool,
                                            @RequestHeader(value = "X-Dify-Token", required = false) String token,
                                            @RequestBody(required = false) Map<String, Object> args) {
        if (props.getDifyToolToken() == null || props.getDifyToolToken().isBlank()
                || !props.getDifyToolToken().equals(token)) {
            throw new BizException(401, "DIFY_TOOL_UNAUTHORIZED：X-Dify-Token 无效或未配置");
        }
        if ("draftIntervention".equals(tool)) {
            throw new BizException(403, "draftIntervention 不进入 Dify");
        }
        LoginUser service = LoginUser.builder()
                .accountId(0L).loginNo("dify-service").role("ADMIN").refId(null).realName("Dify Service")
                .build();
        ToolResult r = registry.execute(tool, args == null ? Map.of() : args, new ToolContext(service, null));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", r.isOk());
        body.put("tool", tool);
        body.put("data", r.getData());
        body.put("evidence", r.getEvidence());
        body.put("errorCode", r.getErrorCode());
        body.put("error", r.getMessage());
        return Result.ok(body);
    }
}
