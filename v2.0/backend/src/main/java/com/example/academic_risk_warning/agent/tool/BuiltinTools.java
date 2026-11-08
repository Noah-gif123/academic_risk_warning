package com.example.academic_risk_warning.agent.tool;

import com.example.academic_risk_warning.service.ai.RetrievalService;
import com.example.academic_risk_warning.service.stats.StatsScopeResolver;
import com.example.academic_risk_warning.service.stats.StatsService;
import com.example.academic_risk_warning.vo.stats.StatsEnvelope;
import com.example.academic_risk_warning.vo.stats.StatsQuery;
import com.example.academic_risk_warning.vo.stats.StatsVos;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P2-2 九个内置工具实现。
 *
 * <p>全部走现有 Service/SQL：统计类复用 P1 的 StatsService/ScopeResolver，
 * 检索复用 RetrievalService；结果统一带 evidence（数字 + 来源）供台账使用。
 * 学生/班级访问均做范围校验（辅导员只看本人教学班/学生）。</p>
 */
@Component
@RequiredArgsConstructor
public class BuiltinTools {

    private final StatsService statsService;
    private final StatsScopeResolver scopeResolver;
    private final RetrievalService retrievalService;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    public Map<String, ToolHandler> handlers() {
        Map<String, ToolHandler> map = new LinkedHashMap<>();
        map.put("getClassRiskStats", this::getClassRiskStats);
        map.put("getRiskTrend", this::getRiskTrend);
        map.put("getCourseCompare", this::getCourseCompare);
        map.put("getStudentRisk", this::getStudentRisk);
        map.put("getInterventions", this::getInterventions);
        map.put("retrieveCourseDocs", this::retrieveCourseDocs);
        map.put("getAlertExplain", this::getAlertExplain);
        map.put("getStudentMemory", this::getStudentMemory);
        map.put("draftIntervention", this::draftIntervention);
        return map;
    }

    // ============================================================ 1) 班级风险
    private ToolResult getClassRiskStats(ToolContext ctx, Map<String, Object> args) {
        Long termId = term(ctx);
        StatsQuery q = query(ctx);
        StatsScopeResolver.Scope scope = scopeResolver.resolve(ctx.user(), q);
        Long teachingClassId = asLong(args.get("teachingClassId"));
        Long adminClassId = asLong(args.get("adminClassId"));
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> evidence = new LinkedHashMap<>();
        if (teachingClassId != null) {
            if (!scope.allScope() && !scope.teachingClassIds().contains(teachingClassId)) {
                return ToolResult.fail("FORBIDDEN", "无权访问该教学班");
            }
            Map<String, Object> row = jdbc.queryForMap(
                    "select count(*) total, coalesce(sum(risk_level='RED'),0) red,"
                            + " coalesce(sum(risk_level='ORANGE'),0) orange, coalesce(sum(risk_level='YELLOW'),0) yellow,"
                            + " coalesce(sum(risk_level='GREEN'),0) green, coalesce(round(avg(risk_score),2),0) avgRisk"
                            + " from risk_state where term_id=? and teaching_class_id=? and is_void=0",
                    termId, teachingClassId);
            data.putAll(row);
            data.put("teachingClassId", teachingClassId);
            data.put("riskRatio", ratio(row));
        } else if (adminClassId != null) {
            if (!scope.allScope() && !scope.classIds().contains(adminClassId)) {
                return ToolResult.fail("FORBIDDEN", "无权访问该行政班");
            }
            Map<String, Object> row = jdbc.queryForMap(
                    "select count(*) total, coalesce(sum(rs.risk_level='RED'),0) red,"
                            + " coalesce(sum(rs.risk_level='ORANGE'),0) orange, coalesce(sum(rs.risk_level='YELLOW'),0) yellow,"
                            + " coalesce(sum(rs.risk_level='GREEN'),0) green, coalesce(round(avg(rs.risk_score),2),0) avgRisk"
                            + " from risk_state rs join student s on s.id=rs.student_id"
                            + " where rs.term_id=? and s.admin_class_id=? and rs.is_void=0",
                    termId, adminClassId);
            data.putAll(row);
            data.put("adminClassId", adminClassId);
            data.put("riskRatio", ratio(row));
        } else {
            StatsEnvelope<StatsVos.Overview> env = statsService.overview(ctx.user(), q);
            StatsVos.Overview o = env.getCurrent();
            data.put("total", o.getTotal());
            data.put("red", o.getRed());
            data.put("orange", o.getOrange());
            data.put("yellow", o.getYellow());
            data.put("green", o.getGreen());
            data.put("riskRatio", o.getRiskRatio());
            data.put("activeAlerts", o.getActiveAlerts());
            data.put("classCount", o.getClassCount());
            data.put("termName", o.getTermName());
        }
        data.put("termId", termId);
        evidence.put("termId", termId);
        evidence.put("total", data.get("total"));
        evidence.put("red", data.get("red"));
        evidence.put("orange", data.get("orange"));
        evidence.put("yellow", data.get("yellow"));
        evidence.put("green", data.get("green"));
        evidence.put("riskRatio", data.get("riskRatio"));
        evidence.put("source", "risk_state");
        return ToolResult.success(data, evidence);
    }

    // ============================================================ 2) 趋势
    private ToolResult getRiskTrend(ToolContext ctx, Map<String, Object> args) {
        StatsQuery q = query(ctx);
        StatsEnvelope<StatsVos.Trend> env = statsService.trend(ctx.user(), q);
        List<StatsVos.TrendPoint> points = env.getCurrent() == null ? List.of() : env.getCurrent().getPoints();
        int weeks = args.get("weeks") == null ? 8 : Math.min(12, ((Number) args.get("weeks")).intValue());
        if (points.size() > weeks) {
            points = points.subList(points.size() - weeks, points.size());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("termId", term(ctx));
        data.put("weeks", weeks);
        data.put("points", points);
        data.put("dataStatus", env.getDataStatus());
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("points", points.size());
        evidence.put("lastWeek", points.isEmpty() ? null : points.get(points.size() - 1).getWeekStart());
        evidence.put("lastRiskRatio", points.isEmpty() ? null : points.get(points.size() - 1).getRiskRatio());
        evidence.put("source", "stat_daily_class + alert_record");
        return ToolResult.success(data, evidence);
    }

    // ============================================================ 3) 对比
    private ToolResult getCourseCompare(ToolContext ctx, Map<String, Object> args) {
        StatsEnvelope<StatsVos.Compare> env = statsService.compare(ctx.user(), query(ctx));
        List<StatsVos.CompareRow> rows = env.getCurrent() == null ? List.of() : env.getCurrent().getRows();
        int topN = args.get("topN") == null ? 10 : Math.min(50, ((Number) args.get("topN")).intValue());
        if (rows.size() > topN) {
            rows = rows.subList(0, topN);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("termId", term(ctx));
        data.put("rows", rows);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("rows", rows.size());
        evidence.put("source", "risk_state");
        return ToolResult.success(data, evidence);
    }

    // ============================================================ 4) 学生风险
    private ToolResult getStudentRisk(ToolContext ctx, Map<String, Object> args) {
        long studentId = ((Number) args.get("studentId")).longValue();
        long termId = term(ctx);
        StatsScopeResolver.Scope scope = scopeResolver.resolve(ctx.user(), query(ctx));
        if (!canAccessStudent(scope, studentId)) {
            return ToolResult.fail("FORBIDDEN", "无权访问该学生");
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select s.id studentId, s.student_no studentNo, s.student_name studentName, rs.risk_level riskLevel,"
                        + " rs.risk_score riskScore, rs.teaching_class_id teachingClassId, tc.class_code classCode,"
                        + " rs.absent_count absentCount, rs.remaining_absent remainingAbsent, rs.hard_fail hardFail,"
                        + " rs.active_channel activeChannel, sr.total_score totalScore"
                        + " from risk_state rs join student s on s.id=rs.student_id"
                        + " left join teaching_class tc on tc.id=rs.teaching_class_id"
                        + " left join score_result sr on sr.enrollment_id=rs.enrollment_id and sr.is_void=0"
                        + " where rs.student_id=? and rs.term_id=? and rs.is_void=0 limit 1", studentId, termId);
        if (rows.isEmpty()) {
            return ToolResult.fail("NOT_FOUND", "学生在该学期没有风险记录");
        }
        Map<String, Object> data = new LinkedHashMap<>(rows.get(0));
        data.put("termId", termId);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("studentId", studentId);
        evidence.put("riskLevel", data.get("riskLevel"));
        evidence.put("riskScore", data.get("riskScore"));
        evidence.put("source", "risk_state");
        return ToolResult.success(data, evidence);
    }

    // ============================================================ 5) 干预
    private ToolResult getInterventions(ToolContext ctx, Map<String, Object> args) {
        long studentId = ((Number) args.get("studentId")).longValue();
        long termId = term(ctx);
        StatsScopeResolver.Scope scope = scopeResolver.resolve(ctx.user(), query(ctx));
        if (!canAccessStudent(scope, studentId)) {
            return ToolResult.fail("FORBIDDEN", "无权访问该学生");
        }
        int limit = args.get("limit") == null ? 10 : Math.min(20, ((Number) args.get("limit")).intValue());
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select ir.id, ir.alert_id alertId, ir.action_type actionType, ir.effective,"
                        + " ir.risk_score_before scoreBefore, ir.risk_score_after scoreAfter,"
                        + " ir.level_before levelBefore, ir.level_after levelAfter, ir.cohort,"
                        + " ir.effect_check_time effectCheckTime, ir.create_time createTime"
                        + " from intervention_record ir where ir.student_id=? and ir.term_id=? and ir.is_void=0"
                        + " order by ir.create_time desc limit ?", studentId, termId, limit);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("studentId", studentId);
        data.put("termId", termId);
        data.put("interventions", rows);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("studentId", studentId);
        evidence.put("interventions", rows.size());
        evidence.put("source", "intervention_record");
        return ToolResult.success(data, evidence);
    }

    // ============================================================ 6) RAG
    private ToolResult retrieveCourseDocs(ToolContext ctx, Map<String, Object> args) {
        long courseId = ((Number) args.get("courseId")).longValue();
        String query = String.valueOf(args.get("query"));
        int topK = args.get("topK") == null ? 5 : Math.min(10, ((Number) args.get("topK")).intValue());
        String mode = args.get("mode") == null ? null : String.valueOf(args.get("mode"));
        RetrievalService.Outcome o = mode == null
                ? retrievalService.retrieve(courseId, query, topK)
                : retrievalService.retrieve(courseId, query, topK, RetrievalService.parseMode(mode));
        List<Map<String, Object>> hits = new ArrayList<>();
        java.util.Map<String, Long> docIdCache = new java.util.HashMap<>();
        for (RetrievalService.Hit h : o.hits()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("chunkId", h.chunkId());
            m.put("docId", docIdCache.computeIfAbsent(h.docName(), name -> {
                List<Long> ids = jdbc.queryForList(
                        "select id from ai_kb_doc where doc_name=? order by id limit 1", Long.class, name);
                return ids.isEmpty() ? null : ids.get(0);
            }));
            m.put("docName", h.docName());
            m.put("sectionNo", h.sectionNo());
            m.put("sourceType", h.sourceType());
            m.put("score", h.fused());
            m.put("rrfRank", h.rank());
            String content = h.content() == null ? "" : h.content();
            m.put("snippet", content.length() <= 120 ? content : content.substring(0, 119) + "…");
            hits.add(m);
        }
        RetrievalService.Guard guard = retrievalService.guard(o, query);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("courseId", courseId);
        data.put("query", query);
        data.put("hits", hits);
        data.put("refused", guard.refuse());
        data.put("gateReason", guard.reason());
        data.put("topChunkId", hits.isEmpty() ? null : hits.get(0).get("chunkId"));
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("courseId", courseId);
        evidence.put("hits", hits.size());
        evidence.put("topChunkId", hits.isEmpty() ? null : hits.get(0).get("chunkId"));
        evidence.put("refused", guard.refuse());
        evidence.put("gateReason", guard.reason());
        evidence.put("source", "ai_kb_chunk");
        return ToolResult.success(data, evidence);
    }

    // ============================================================ 7) 预警解释
    private ToolResult getAlertExplain(ToolContext ctx, Map<String, Object> args) {
        long alertId = ((Number) args.get("alertId")).longValue();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select id, alert_id alertId, summary, content_json contentJson, status, source, model, create_time createTime"
                        + " from ai_alert_explain where alert_id=? order by id desc limit 1", alertId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("alertId", alertId);
        data.put("found", !rows.isEmpty());
        if (!rows.isEmpty()) {
            data.putAll(rows.get(0));
        } else {
            data.put("note", "该预警尚未生成解释卡（P2 运行后可生成）");
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("alertId", alertId);
        evidence.put("found", !rows.isEmpty());
        evidence.put("source", "ai_alert_explain");
        return ToolResult.success(data, evidence);
    }

    // ============================================================ 8) 记忆
    private ToolResult getStudentMemory(ToolContext ctx, Map<String, Object> args) {
        long studentId = ((Number) args.get("studentId")).longValue();
        int exists = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema=database() and table_name='student_memory'",
                Integer.class);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("studentId", studentId);
        if (exists == 0) {
            data.put("items", List.of());
            data.put("note", "student_memory 尚未建表（P2-11 后可用）");
        } else {
            data.put("items", jdbc.queryForList(
                    "select summary, updated_at updatedAt from student_memory where student_id=?", studentId));
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("studentId", studentId);
        evidence.put("items", ((List<?>) data.get("items")).size());
        evidence.put("source", "student_memory");
        return ToolResult.success(data, evidence);
    }

    // ============================================================ 9) 干预草稿
    private ToolResult draftIntervention(ToolContext ctx, Map<String, Object> args) {
        long studentId = ((Number) args.get("studentId")).longValue();
        long termId = term(ctx);
        StatsScopeResolver.Scope scope = scopeResolver.resolve(ctx.user(), query(ctx));
        if (!canAccessStudent(scope, studentId)) {
            return ToolResult.fail("FORBIDDEN", "无权访问该学生");
        }
        String reason = String.valueOf(args.get("reason"));
        String actionType = args.get("actionType") == null ? "TALK" : String.valueOf(args.get("actionType"));
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select risk_level riskLevel, risk_score riskScore, teaching_class_id teachingClassId"
                        + " from risk_state where student_id=? and term_id=? and is_void=0 limit 1", studentId, termId);
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("studentId", studentId);
        draft.put("termId", termId);
        draft.put("actionType", actionType);
        draft.put("reason", reason);
        draft.put("riskLevel", rows.isEmpty() ? null : rows.get(0).get("riskLevel"));
        draft.put("riskScore", rows.isEmpty() ? null : rows.get(0).get("riskScore"));
        draft.put("requiresHumanApproval", true);
        draft.put("status", "DRAFT_NOT_PERSISTED");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("draft", draft);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("studentId", studentId);
        evidence.put("draft", draft.get("status"));
        evidence.put("source", "risk_state");
        return ToolResult.success(data, evidence);
    }

    // ============================================================ helpers
    private StatsQuery query(ToolContext ctx) {
        StatsQuery q = new StatsQuery();
        q.setTermId(ctx.termId());
        q.setPeriod("term");
        return q;
    }

    private long term(ToolContext ctx) {
        if (ctx.termId() != null) {
            return ctx.termId();
        }
        Long id = jdbc.queryForObject(
                "select id from term where is_current=1 order by id desc limit 1", Long.class);
        return id == null ? 5L : id;
    }

    private boolean canAccessStudent(StatsScopeResolver.Scope scope, long studentId) {
        if (scope.allScope()) {
            return true;
        }
        List<Long> adminClass = jdbc.queryForList(
                "select admin_class_id from student where id=?", Long.class, studentId);
        return !adminClass.isEmpty() && scope.classIds().contains(adminClass.get(0));
    }

    private Long asLong(Object v) {
        return v == null ? null : ((Number) v).longValue();
    }

    private Object ratio(Map<String, Object> row) {
        int red = ((Number) row.getOrDefault("red", 0)).intValue();
        int orange = ((Number) row.getOrDefault("orange", 0)).intValue();
        int yellow = ((Number) row.getOrDefault("yellow", 0)).intValue();
        int total = ((Number) row.getOrDefault("total", 0)).intValue();
        return total == 0 ? null : Math.round((red + orange + yellow) * 10000.0 / total) / 100.0;
    }
}
