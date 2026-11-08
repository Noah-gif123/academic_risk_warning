package com.example.academic_risk_warning.agent.trace;

import com.example.academic_risk_warning.agent.ledger.EvidenceLedger;
import com.example.academic_risk_warning.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * P2-12 轨迹服务：ai_run / ai_run_step 落库。
 *
 * <p>记录：运行（scene/provider/role_count/run_key）、每步（角色/工具/入参摘要/
 * 返回摘要/门控状态/耗时）；轨迹详情同时返回证据台账，供数字回溯到来源步。</p>
 */
@Service
@RequiredArgsConstructor
public class AgentTraceService {

    private final JdbcTemplate jdbc;
    private final EvidenceLedger ledger;

    public record Start(long runId, String runKey) {
    }

    public Start start(String scene, String question, LoginUser user, Long termId) {
        try {
            String runKey = ledger.newRunKey();
            KeyHolder key = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "insert into ai_run (scene, target_type, target_id, status, source, model, provider,"
                                + " role_count, run_key, operator_id, create_time) values (?,?,?,?,?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, scene);
                ps.setString(2, "QUESTION");
                ps.setLong(3, 0L);
                ps.setString(4, "RUNNING");
                ps.setString(5, "LOCAL");
                ps.setString(6, "baseline-agent");
                ps.setString(7, "LOCAL");
                ps.setInt(8, 0);
                ps.setString(9, runKey);
                ps.setObject(10, user == null ? null : user.getAccountId());
                ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
                return ps;
            }, key);
            return new Start(key.getKey().longValue(), runKey);
        } catch (Exception e) {
            return null;
        }
    }

    public void step(long runId, int stepNo, String role, String tool, String argsJson,
                     String resultJson, String gateStatus, long durationMs, String detail) {
        try {
            jdbc.update("insert into ai_run_step (run_id, step_no, name, input_digest, output_digest,"
                            + " validation_status, validation_detail, duration_ms, create_time,"
                            + " step_role, tool_name, args_digest, result_digest, gate_status)"
                            + " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    runId, stepNo, role, digest(argsJson), digest(resultJson), gateStatus, detail,
                    durationMs, Timestamp.valueOf(LocalDateTime.now()), role, tool,
                    digest(argsJson), digest(resultJson), gateStatus);
        } catch (Exception ignored) {
            // 轨迹失败不影响主流程
        }
    }

    public void finish(long runId, String status, int roleCount) {
        try {
            jdbc.update("update ai_run set status=?, role_count=?, finish_time=? where id=?",
                    status, roleCount, Timestamp.valueOf(LocalDateTime.now()), runId);
        } catch (Exception ignored) {
        }
    }

    public List<Map<String, Object>> list(int limit) {
        return jdbc.queryForList(
                "select id, scene, status, provider, role_count roleCount, run_key runKey,"
                        + " operator_id operatorId, create_time createTime, finish_time finishTime"
                        + " from ai_run order by id desc limit ?", Math.max(1, Math.min(100, limit)));
    }

    public Map<String, Object> detail(long runId) {
        Map<String, Object> run = jdbc.queryForMap(
                "select id, scene, status, provider, role_count roleCount, run_key runKey,"
                        + " operator_id operatorId, create_time createTime, finish_time finishTime"
                        + " from ai_run where id=?", runId);
        List<Map<String, Object>> steps = jdbc.queryForList(
                "select step_no stepNo, step_role stepRole, tool_name toolName, name,"
                        + " input_digest inputDigest, output_digest outputDigest,"
                        + " args_digest argsDigest, result_digest resultDigest,"
                        + " validation_status validationStatus, validation_detail validationDetail,"
                        + " duration_ms durationMs, gate_status gateStatus, create_time createTime"
                        + " from ai_run_step where run_id=? order by step_no, id", runId);
        Object runKey = run.get("runKey");
        run.put("steps", steps);
        run.put("ledger", runKey == null ? List.of() : ledger.entries(String.valueOf(runKey)));
        return run;
    }

    private static String digest(String s) {
        if (s == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 16);
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }
}
