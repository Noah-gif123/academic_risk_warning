package com.example.academic_risk_warning.agent.memory;

import com.example.academic_risk_warning.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P2-11 学生记忆：类型开关 + 摘要/事件流 + 学生可查可删。
 *
 * <p>高权重类型（RISK_CHANGE / INTERVENTION）进入摘要；低权重只进事件流。
 * 删除后摘要与事件都删除，getStudentMemory 不再返回。</p>
 */
@Service
@RequiredArgsConstructor
public class MemoryService {

    private static final int SUMMARY_MAX = 300;

    private final JdbcTemplate jdbc;

    public Map<String, Object> config() {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("enabled", true);
        c.put("summaryMaxChars", SUMMARY_MAX);
        c.put("types", Map.of(
                "RISK_CHANGE", Map.of("enabled", true, "weight", "HIGH"),
                "INTERVENTION", Map.of("enabled", true, "weight", "HIGH"),
                "QA", Map.of("enabled", true, "weight", "LOW"),
                "QUESTIONNAIRE", Map.of("enabled", true, "weight", "LOW")
        ));
        return c;
    }

    /** 写事件（按类型开关/权重）。 */
    @Transactional
    public void record(long studentId, String type, String summary, String sourceId) {
        String weight = switch (type) {
            case "RISK_CHANGE", "INTERVENTION" -> "HIGH";
            case "QA", "QUESTIONNAIRE" -> "LOW";
            default -> null;
        };
        if (weight == null) {
            return;
        }
        jdbc.update("insert into student_memory_event (student_id, type, summary, source_id, weight, created_at)"
                + " values (?,?,?,?,?,now())", studentId, type, truncate(summary), sourceId, weight);
        rebuild(studentId);
    }

    /** 从风险/干预事实重建摘要（管理员演示入口）。 */
    @Transactional
    public Map<String, Object> rebuildFromFacts(long studentId) {
        List<Map<String, Object>> risk = jdbc.queryForList(
                "select risk_level riskLevel, risk_score riskScore, absent_count absentCount,"
                        + " remaining_absent remainingAbsent from risk_state"
                        + " where student_id=? and is_void=0 order by id desc limit 1", studentId);
        if (risk.isEmpty()) {
            throw new BizException(404, "学生没有风险记录");
        }
        Map<String, Object> r = risk.get(0);
        int interventions = jdbc.queryForObject(
                "select count(*) from intervention_record where student_id=? and is_void=0",
                Integer.class, studentId);
        String lastConclusion = jdbc.queryForList(
                "select concat(coalesce(content,''),' / ',coalesce(result,'')) text from intervention_record"
                        + " where student_id=? and is_void=0 order by create_time desc limit 1", String.class, studentId)
                .stream().findFirst().orElse("");
        String summary = "当前风险等级 " + r.get("riskLevel") + "（" + r.get("riskScore") + " 分）；"
                + "累计干预 " + interventions + " 次；缺课 " + r.get("absentCount")
                + " 次（剩余可缺 " + r.get("remainingAbsent") + "）"
                + (lastConclusion.isBlank() ? "" : "；上次干预：" + lastConclusion);
        jdbc.update("insert into student_memory_event (student_id, type, summary, source_id, weight, created_at)"
                + " values (?, 'RISK_CHANGE', ?, 'rebuild', 'HIGH', now())",
                studentId, truncate(summary));
        jdbc.update("insert into student_memory (student_id, summary, top_weak_groups, attendance_tag,"
                        + " intervention_count, last_intervention_conclusion, recent_levels, updated_at)"
                        + " values (?,?,json_array(),?,?,?,?,now())"
                        + " on duplicate key update summary=values(summary), attendance_tag=values(attendance_tag),"
                        + " intervention_count=values(intervention_count),"
                        + " last_intervention_conclusion=values(last_intervention_conclusion),"
                        + " recent_levels=values(recent_levels), updated_at=now()",
                studentId, truncate(summary),
                r.get("absentCount") == null || ((Number) r.get("absentCount")).intValue() == 0 ? "正常" : "缺课关注",
                interventions, lastConclusion, String.valueOf(r.get("riskLevel")));
        return get(studentId);
    }

    /** 从事件流重建摘要（高权重事件拼接，≤300 字）。 */
    public void rebuild(long studentId) {
        List<String> high = jdbc.queryForList(
                "select summary from student_memory_event where student_id=? and weight='HIGH'"
                        + " order by created_at desc, id desc limit 5", String.class, studentId);
        StringBuilder sb = new StringBuilder();
        for (String s : high) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(s);
        }
        String summary = truncate(sb.toString());
        int count = jdbc.queryForObject(
                "select count(*) from intervention_record where student_id=? and is_void=0",
                Integer.class, studentId);
        String last = jdbc.queryForList(
                "select concat(coalesce(content,''),' / ',coalesce(result,'')) from intervention_record"
                        + " where student_id=? and is_void=0 order by create_time desc limit 1", String.class, studentId)
                .stream().findFirst().orElse("");
        jdbc.update("insert into student_memory (student_id, summary, intervention_count,"
                        + " last_intervention_conclusion, updated_at) values (?,?,?,?,now())"
                        + " on duplicate key update summary=values(summary), intervention_count=values(intervention_count),"
                        + " last_intervention_conclusion=values(last_intervention_conclusion), updated_at=now()",
                studentId, summary, count, truncate(last));
    }

    public Map<String, Object> get(long studentId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select student_id studentId, summary, top_weak_groups topWeakGroups, attendance_tag attendanceTag,"
                        + " intervention_count interventionCount, last_intervention_conclusion lastConclusion,"
                        + " recent_levels recentLevels, updated_at updatedAt"
                        + " from student_memory where student_id=?", studentId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("studentId", studentId);
        out.put("exists", !rows.isEmpty());
        out.put("memory", rows.isEmpty() ? null : rows.get(0));
        out.put("events", jdbc.queryForList(
                "select id, type, summary, source_id sourceId, weight, created_at createdAt"
                        + " from student_memory_event where student_id=? order by id desc limit 20", studentId));
        out.put("config", config());
        return out;
    }

    @Transactional
    public Map<String, Object> delete(long studentId) {
        int events = jdbc.update("delete from student_memory_event where student_id=?", studentId);
        int memory = jdbc.update("delete from student_memory where student_id=?", studentId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("studentId", studentId);
        out.put("deletedEvents", events);
        out.put("deletedMemory", memory);
        out.put("note", "删除后 getStudentMemory 不再返回，事件流不再注入");
        return out;
    }

    private String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= SUMMARY_MAX ? s : s.substring(0, SUMMARY_MAX);
    }
}
