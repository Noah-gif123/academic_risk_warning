package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * AI 运行记录与限额：每一次调用都留痕（谁、什么场景、花了多少 token）。
 *
 * <p>这是答辩时"AI 可追溯、成本可见"的物证，也是防止额度跑飞的闸门。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRunService {

    private final JdbcTemplate jdbc;
    private final AiProperties props;

    /** 开一次运行 */
    public Long start(String scene, String targetType, Long targetId, Long operatorId) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "insert into ai_run(scene, target_type, target_id, status, model, prompt_version, operator_id) "
                            + "values (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, scene);
            ps.setString(2, targetType);
            if (targetId == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, targetId);
            }
            ps.setString(4, "RUNNING");
            ps.setString(5, props.getModel());
            ps.setString(6, "REPORT".equals(scene) ? props.getReportPromptVersion() : null);
            if (operatorId == null) {
                ps.setNull(7, java.sql.Types.BIGINT);
            } else {
                ps.setLong(7, operatorId);
            }
            return ps;
        }, kh);
        return kh.getKey() == null ? null : kh.getKey().longValue();
    }

    /** 结束一次运行 */
    public void finish(Long runId, String status, String source, Integer tokens, String error) {
        if (runId == null) {
            return;
        }
        double cost = props.costOf(tokens);
        // model 要跟着实际通道走：写成配置里的演示标签（qwen-plus）会把"其实走的是 Dify/deepseek"记错
        String model = modelLabel(source);
        jdbc.update("update ai_run set status=?, source=?, model=?, total_tokens=?, total_cost=?, error_message=?, finish_time=? where id=?",
                status, source, model, tokens == null ? 0 : tokens, BigDecimal.valueOf(cost),
                error == null ? null : truncate(error, 480), LocalDateTime.now(), runId);
        // usage 通常先于 finish 落库（那时还不知道通道），这里按实际通道回填一次
        try {
            jdbc.update("update ai_usage set model=? where run_id=?", model, runId);
        } catch (Exception e) {
            log.debug("[ai_usage] 回填模型失败: {}", e.getMessage());
        }
    }

    /**
     * 实际使用的模型标签。
     *
     * <p>DIFY 通道的模型由 Dify 工作流自己决定，后端并不知道具体名字，因此记 {@code dify-workflow}
     * 而不是配置里的演示标签；直连通道记真实模型名；规则模板没有模型，记 null。</p>
     */
    public String modelLabel(String source) {
        if (source == null) {
            return props.getModel();
        }
        return switch (source) {
            case "DIFY" -> "dify-workflow";
            case "LLM" -> {
                String m = props.directModel();
                yield (m == null || m.isBlank()) ? props.getModel() : m;
            }
            case "TEMPLATE" -> null;
            default -> props.getModel();
        };
    }

    /** 记录一步 */
    public void step(Long runId, int stepNo, String name, String input, String output,
                     String validationStatus, String validationDetail, long durationMs) {
        if (runId == null) {
            return;
        }
        try {
            jdbc.update("insert into ai_run_step(run_id, step_no, name, input_digest, output_digest, validation_status, validation_detail, duration_ms) "
                            + "values (?,?,?,?,?,?,?,?)",
                    runId, stepNo, name, truncate(input, 480), truncate(output, 480),
                    validationStatus, truncate(validationDetail, 480), (int) durationMs);
        } catch (Exception e) {
            log.debug("[ai_run_step] 写入失败: {}", e.getMessage());
        }
    }

    /** 记录一步（带角色/工具，报告流水线用） */
    public void stepWithRole(Long runId, int stepNo, String name, String stepRole, String toolName,
                             String input, String output, String validationStatus, String validationDetail, long durationMs) {
        if (runId == null) {
            return;
        }
        try {
            jdbc.update("insert into ai_run_step(run_id, step_no, name, step_role, tool_name, input_digest, output_digest, "
                            + "validation_status, validation_detail, duration_ms) values (?,?,?,?,?,?,?,?,?,?)",
                    runId, stepNo, name, stepRole, toolName, truncate(input, 480), truncate(output, 480),
                    validationStatus, truncate(validationDetail, 480), (int) durationMs);
        } catch (Exception e) {
            log.debug("[ai_run_step] 写入失败: {}", e.getMessage());
        }
    }

    /** 计费明细 */
    public void usage(Long runId, String scene, Integer tokens, Long operatorId) {
        try {
            String source = null;
            try {
                source = jdbc.queryForObject("select source from ai_run where id=?", String.class, runId);
            } catch (Exception ignore) {
                // 运行还没落 source 时退回配置标签
            }
            jdbc.update("insert into ai_usage(run_id, scene, model, prompt_tokens, completion_tokens, cost, operator_id) values (?,?,?,?,?,?,?)",
                    runId, scene, modelLabel(source), null, tokens,
                    BigDecimal.valueOf(props.costOf(tokens)), operatorId);
        } catch (Exception e) {
            log.debug("[ai_usage] 写入失败: {}", e.getMessage());
        }
    }

    /** 当天已调用次数（用于限额） */
    public int todayCount(Long operatorId, String scene) {
        Integer n = jdbc.queryForObject(
                "select count(*) from ai_run where operator_id = ? and scene = ? and create_time >= curdate()",
                Integer.class, operatorId, scene);
        return n == null ? 0 : n;
    }

    public boolean overLimit(Long operatorId, String scene) {
        return props.getDailyLimit() > 0 && todayCount(operatorId, scene) >= props.getDailyLimit();
    }

    /** 今日/本月用量汇总（给管理端与成本看板） */
    public java.util.Map<String, Object> summary() {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("todayCalls", jdbc.queryForObject("select count(*) from ai_run where create_time >= curdate()", Integer.class));
        m.put("todayTokens", jdbc.queryForObject("select coalesce(sum(total_tokens),0) from ai_run where create_time >= curdate()", Integer.class));
        m.put("monthCost", jdbc.queryForObject(
                "select coalesce(sum(cost),0) from ai_usage where create_time >= date_format(curdate(), '%Y-%m-01')", BigDecimal.class));
        m.put("difySuccess", jdbc.queryForObject("select count(*) from ai_run where source='DIFY' and status='SUCCESS'", Integer.class));
        m.put("templateFallback", jdbc.queryForObject("select count(*) from ai_run where status='FALLBACK'", Integer.class));
        return m;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
