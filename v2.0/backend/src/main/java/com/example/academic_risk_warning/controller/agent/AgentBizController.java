package com.example.academic_risk_warning.controller.agent;

import com.example.academic_risk_warning.agent.biz.DiagnosticianAgent;
import com.example.academic_risk_warning.agent.biz.GraderAgent;
import com.example.academic_risk_warning.agent.biz.ReporterAgent;
import com.example.academic_risk_warning.agent.biz.TutorAgent;
import com.example.academic_risk_warning.agent.dify.DifyAgentProvider;
import com.example.academic_risk_warning.agent.rag.CitationBinder;
import com.example.academic_risk_warning.agent.tool.ToolContext;
import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * P2-7 ~ P2-10 四个业务智能体入口。
 *
 * <p>权限按 Agent 分别校验：Diagnostician=教务/辅导员；Reporter=教师/辅导员；
 * Grader=教师/教务；Tutor=学生/教师/教务。</p>
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentBizController {

    private final DiagnosticianAgent diagnostician;
    private final ReporterAgent reporter;
    private final GraderAgent grader;
    private final TutorAgent tutor;
    private final CitationBinder citationBinder;
    private final DifyAgentProvider difyProvider;

    @PostMapping("/diagnose")
    @RequireRole({"ADMIN", "COUNSELOR"})
    public Result<Map<String, Object>> diagnose(@RequestBody Map<String, Object> body) {
        String question = body.get("question") == null ? "" : String.valueOf(body.get("question")).trim();
        if (question.isEmpty()) {
            throw new BizException(400, "question 不能为空");
        }
        Long termId = body.get("termId") == null ? null : ((Number) body.get("termId")).longValue();
        String provider = body.get("provider") == null ? "LOCAL" : String.valueOf(body.get("provider")).toUpperCase();
        boolean strictDify = Boolean.TRUE.equals(body.get("strictDify"));
        LoginUser user = UserContext.require();
        ToolContext ctx = new ToolContext(user, termId);
        if ("DIFY".equals(provider)) {
            try {
                return Result.ok(difyProvider.diagnose(question, ctx));
            } catch (Exception e) {
                if (strictDify) {
                    throw new BizException(503, "DIFY_FAILED: " + e.getMessage());
                }
                Map<String, Object> fallback = diagnostician.diagnose(question, ctx);
                fallback.put("requestedProvider", "DIFY");
                fallback.put("provider", "LOCAL");
                fallback.put("fallbackReason", e.getMessage());
                fallback.put("difyConfigured", difyProvider.configured());
                return Result.ok(fallback);
            }
        }
        if (!"LOCAL".equals(provider)) {
            throw new BizException(400, "provider 仅支持 LOCAL/DIFY");
        }
        Map<String, Object> local = diagnostician.diagnose(question, ctx);
        local.put("requestedProvider", "LOCAL");
        local.put("provider", "LOCAL");
        return Result.ok(local);
    }

    /** P3 契约 `/v1/run`：供 Dify 自定义工具/演示按 diagnostician_openapi.yaml 调用。 */
    @PostMapping("/v1/run")
    @RequireRole({"ADMIN", "COUNSELOR"})
    public Result<Map<String, Object>> runV1(@RequestBody Map<String, Object> body) {
        return diagnose(body);
    }

    /** P3 健康探针：Dify 是否配置/可达；离线时 parity 脚本据此 skip。 */
    @GetMapping("/dify/health")
    @RequireRole({"ADMIN", "COUNSELOR"})
    public Result<Map<String, Object>> difyHealth() {
        return Result.ok(difyProvider.health());
    }

    @PostMapping("/report")
    @RequireRole({"TEACHER", "COUNSELOR", "ADMIN"})
    public Result<Map<String, Object>> report(@RequestBody Map<String, Object> body) {
        String type = body.get("type") == null ? "" : String.valueOf(body.get("type"));
        if (body.get("targetId") == null) {
            throw new BizException(400, "targetId 不能为空");
        }
        Long targetId = ((Number) body.get("targetId")).longValue();
        return Result.ok(reporter.generate(type, targetId, UserContext.require()));
    }

    @PostMapping("/grade")
    @RequireRole({"TEACHER", "ADMIN"})
    public Result<Map<String, Object>> grade(@RequestBody Map<String, Object> body) {
        if (body.get("submissionId") == null) {
            throw new BizException(400, "submissionId 不能为空");
        }
        long submissionId = ((Number) body.get("submissionId")).longValue();
        return Result.ok(grader.gradeSubmission(submissionId));
    }

    @PostMapping("/grade/eval")
    @RequireRole({"TEACHER", "ADMIN"})
    public Result<Map<String, Object>> gradeEval() {
        return Result.ok(grader.evalAll());
    }

    @PostMapping("/grade/apply")
    @RequireRole({"TEACHER", "ADMIN"})
    public Result<Map<String, Object>> gradeApply(@RequestBody Map<String, Object> body) {
        if (body.get("answerId") == null || body.get("score") == null) {
            throw new BizException(400, "answerId/score 不能为空");
        }
        long answerId = ((Number) body.get("answerId")).longValue();
        BigDecimal score = new BigDecimal(String.valueOf(body.get("score")));
        String comment = body.get("comment") == null ? null : String.valueOf(body.get("comment"));
        LoginUser user = UserContext.require();
        return Result.ok(grader.apply(answerId, score, comment, user.getRefId()));
    }

    /** P2R-2 轨迹页点开原文片段。 */
    @GetMapping("/kb/chunk/{chunkId}")
    @RequireRole({"STUDENT", "TEACHER", "ADMIN", "COUNSELOR"})
    public Result<Map<String, Object>> chunk(@PathVariable long chunkId) {
        Map<String, Object> chunk = citationBinder.chunk(chunkId);
        if (chunk == null) {
            throw new BizException(404, "资料片段不存在：" + chunkId);
        }
        return Result.ok(chunk);
    }

    @PostMapping("/tutor")
    @RequireRole({"STUDENT", "TEACHER", "ADMIN"})
    public Result<Map<String, Object>> tutor(@RequestBody Map<String, Object> body) {
        if (body.get("courseId") == null || body.get("question") == null) {
            throw new BizException(400, "courseId/question 不能为空");
        }
        long courseId = ((Number) body.get("courseId")).longValue();
        String question = String.valueOf(body.get("question"));
        return Result.ok(tutor.tutor(courseId, question, UserContext.require()));
    }
}
