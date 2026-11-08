package com.example.academic_risk_warning.service.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * P1 物化任务（硬化版）。
 *
 * <p>相对 P1-3：
 * <ul>
 *   <li>三表全部按 term_id 读写（多学期）；</li>
 *   <li>周 KPI 分 scope：ALL / COUNSELOR / CLASS（COURSE 因无日快照数据暂缓）；</li>
 *   <li>日快照带 source（MATERIALIZED / DEMO_BACKFILL），支持真日累积；</li>
 *   <li>失效校验返回 RefreshResult（refreshed / staleBefore / reason），供接口透出。</li>
 * </ul></p>
 */
@Service
@RequiredArgsConstructor
public class StatsMaterializer {

    private final JdbcTemplate jdbc;

    /** 读取前刷新结果。 */
    public record RefreshResult(boolean refreshed, boolean staleBefore, String reason, String detail) {
    }

    @Scheduled(cron = "0 10 2 * * ?")
    public void scheduledRefresh() {
        try {
            Long termId = jdbc.queryForObject(
                    "select id from term where is_current = 1 order by id desc limit 1", Long.class);
            if (termId != null) {
                refresh(termId);
            }
        } catch (Exception ignored) {
            // 定时任务失败不阻塞应用；读路径 ensureFresh 会补
        }
    }

    public RefreshResult ensureFresh(long termId) {
        String reason = staleReason(termId);
        if (reason == null) {
            return new RefreshResult(false, false, null, "fresh");
        }
        String detail = refresh(termId);
        return new RefreshResult(true, true, reason, detail);
    }

    public String staleReason(long termId) {
        Integer rows = jdbc.queryForObject(
                "select count(*) from stat_daily_class where term_id = ?", Integer.class, termId);
        if (rows == null || rows == 0) {
            return "stat_daily_class 为空";
        }
        Map<String, Object> last = jdbc.queryForMap(
                "select engine_version, source_max_update_time, is_stale from stat_daily_class"
                        + " where term_id = ? order by stat_date desc, id desc limit 1", termId);
        Object stale = last.get("is_stale");
        if (isTrue(stale)) {
            return "is_stale=1";
        }
        Map<String, Object> cur = currentSource(termId);
        String statEngine = (String) last.get("engine_version");
        if (statEngine == null || !statEngine.equals(cur.get("engine_version"))) {
            return "engine_version 不一致";
        }
        LocalDateTime statTime = toLdt(last.get("source_max_update_time"));
        LocalDateTime curTime = toLdt(cur.get("source_max_update_time"));
        if (statTime == null || (curTime != null && curTime.isAfter(statTime))) {
            return "源数据已更新";
        }
        return null;
    }

    @Transactional
    public String refresh(long termId) {
        Map<String, Object> src = currentSource(termId);
        String engine = (String) src.get("engine_version");
        LocalDateTime sourceTime = toLdt(src.get("source_max_update_time"));
        int daily = upsertDaily(termId);
        int weeks = refreshWeeklyKpi(termId, engine, sourceTime);
        int effects = refreshInterventionEffect(termId, engine, sourceTime);
        jdbc.update("update stat_daily_class set engine_version = ?, source_max_update_time = ?, is_stale = 0 where term_id = ?",
                engine, sourceTime, termId);
        jdbc.update("update stat_weekly_kpi set engine_version = ?, source_max_update_time = ?, is_stale = 0 where term_id = ?",
                engine, sourceTime, termId);
        jdbc.update("update stat_intervention_effect set engine_version = ?, source_max_update_time = ?, is_stale = 0 where term_id = ?",
                engine, sourceTime, termId);
        return "daily=" + daily + ", weeks=" + weeks + ", effects=" + effects;
    }

    // ============================================================ 日快照
    private int upsertDaily(long termId) {
        LocalDate today = LocalDate.now();
        jdbc.update("INSERT INTO stat_daily_class (term_id, stat_date, teaching_class_id, student_count, red, orange,"
                        + " yellow, green, avg_score, alert_new, alert_closed, intervention_count, engine_version,"
                        + " source_max_update_time, is_stale, source, create_time, update_time)"
                        + " SELECT ?, ?, rs.teaching_class_id, COUNT(*),"
                        + " COALESCE(SUM(rs.risk_level='RED'),0), COALESCE(SUM(rs.risk_level='ORANGE'),0),"
                        + " COALESCE(SUM(rs.risk_level='YELLOW'),0), COALESCE(SUM(rs.risk_level='GREEN'),0),"
                        + " ROUND(AVG(rs.risk_score),2),"
                        + " (SELECT COUNT(*) FROM alert_record ar WHERE ar.teaching_class_id = rs.teaching_class_id"
                        + "   AND ar.term_id = rs.term_id AND ar.is_void = 0 AND DATE(ar.create_time) = ?),"
                        + " (SELECT COUNT(*) FROM alert_record ar WHERE ar.teaching_class_id = rs.teaching_class_id"
                        + "   AND ar.term_id = rs.term_id AND ar.is_void = 0 AND DATE(ar.close_time) = ?),"
                        + " (SELECT COUNT(*) FROM intervention_record ir JOIN alert_record ar2 ON ar2.id = ir.alert_id"
                        + "   WHERE ar2.teaching_class_id = rs.teaching_class_id AND ar2.term_id = rs.term_id"
                        + "   AND ir.is_void = 0 AND DATE(ir.create_time) = ?),"
                        + " NULL, NULL, 0, 'MATERIALIZED', NOW(), NOW()"
                        + " FROM risk_state rs WHERE rs.term_id = ? AND rs.is_void = 0 GROUP BY rs.teaching_class_id"
                        + " ON DUPLICATE KEY UPDATE student_count = VALUES(student_count), red = VALUES(red),"
                        + " orange = VALUES(orange), yellow = VALUES(yellow), green = VALUES(green),"
                        + " avg_score = VALUES(avg_score), alert_new = VALUES(alert_new),"
                        + " alert_closed = VALUES(alert_closed), intervention_count = VALUES(intervention_count),"
                        + " source = 'MATERIALIZED', update_time = NOW()",
                termId, today, today, today, today, termId);
        return jdbc.queryForObject("select count(*) from stat_daily_class where term_id = ? and stat_date = ?",
                Integer.class, termId, today);
    }

    // ============================================================ 周 KPI（ALL/COUNSELOR/CLASS）
    private static class Agg {
        double hot, total, created, closed, hoursSum, closedCnt, due, improved;

        double riskRatio() {
            return total == 0 ? Double.NaN : round(hot * 100.0 / total);
        }

        double closeRate() {
            return created == 0 ? Double.NaN : round(closed * 100.0 / created);
        }

        double avgHours() {
            return closedCnt == 0 ? Double.NaN : round(hoursSum / closedCnt);
        }

        double improveRate() {
            return due == 0 ? Double.NaN : round(improved * 100.0 / due);
        }
    }

    private int refreshWeeklyKpi(long termId, String engine, LocalDateTime sourceTime) {
        jdbc.update("delete from stat_weekly_kpi where term_id = ?", termId);
        List<LocalDate> weeks = weeklyStarts(termId);
        if (weeks.isEmpty()) {
            return 0;
        }
        Map<String, Map<Long, Agg>> byWeek = new LinkedHashMap<>();
        for (LocalDate w : weeks) {
            byWeek.put(w.toString(), new LinkedHashMap<>());
        }
        // 风险面（班级日快照）
        for (Map<String, Object> r : jdbc.queryForList(
                "select date_format(date_sub(stat_date, interval weekday(stat_date) day), '%Y-%m-%d') wk,"
                        + " teaching_class_id, sum(red+orange+yellow) hot, sum(student_count) total"
                        + " from stat_daily_class where term_id = ? group by wk, teaching_class_id", termId)) {
            Agg a = agg(byWeek, r, "wk", "teaching_class_id");
            a.hot += d(r, "hot");
            a.total += d(r, "total");
        }
        // 关闭率
        for (Map<String, Object> r : jdbc.queryForList(
                "select date_format(date_sub(create_time, interval weekday(create_time) day), '%Y-%m-%d') wk,"
                        + " teaching_class_id, count(*) created, sum(close_time is not null) closed"
                        + " from alert_record where term_id = ? and is_void = 0 group by wk, teaching_class_id", termId)) {
            Agg a = agg(byWeek, r, "wk", "teaching_class_id");
            a.created += d(r, "created");
            a.closed += d(r, "closed");
        }
        // 平均时长
        for (Map<String, Object> r : jdbc.queryForList(
                "select date_format(date_sub(close_time, interval weekday(close_time) day), '%Y-%m-%d') wk,"
                        + " teaching_class_id, sum(timestampdiff(minute, create_time, close_time)) / 60.0 hours,"
                        + " count(*) cnt from alert_record"
                        + " where term_id = ? and is_void = 0 and close_time is not null group by wk, teaching_class_id",
                termId)) {
            Agg a = agg(byWeek, r, "wk", "teaching_class_id");
            a.hoursSum += d(r, "hours");
            a.closedCnt += d(r, "cnt");
        }
        // 干预改善（T+7 等级口径）
        String valid = "ir.level_after is not null and ir.effect_check_time between ir.create_time + interval 7 day"
                + " and ir.create_time + interval 14 day";
        String rankAfter = "(case ir.level_after when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)";
        String rankBefore = "(case ir.level_before when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)";
        for (Map<String, Object> r : jdbc.queryForList(
                "select date_format(date_sub(ir.create_time, interval weekday(ir.create_time) day), '%Y-%m-%d') wk,"
                        + " ar.teaching_class_id, sum(" + valid + ") due,"
                        + " sum(" + valid + " and " + rankAfter + " < " + rankBefore + ") improved"
                        + " from intervention_record ir join alert_record ar on ar.id = ir.alert_id"
                        + " where ir.term_id = ? and ir.is_void = 0 and ir.cohort is not null"
                        + " group by wk, ar.teaching_class_id", termId)) {
            Agg a = agg(byWeek, r, "wk", "teaching_class_id");
            a.due += d(r, "due");
            a.improved += d(r, "improved");
        }
        Map<Long, Long> classCounselor = classCounselor(termId);
        Map<String, Double> prev = new HashMap<>();
        int n = 0;
        for (LocalDate w : weeks) {
            String key = w.toString();
            Map<Long, Agg> classes = byWeek.get(key);
            // CLASS
            for (Map.Entry<Long, Agg> e : classes.entrySet()) {
                Agg a = e.getValue();
                n += putKpi(termId, w, "CLASS", e.getKey(), a, prev, engine, sourceTime);
            }
            // ALL
            Agg all = rollup(classes.values());
            n += putKpi(termId, w, "ALL", 0, all, prev, engine, sourceTime);
            n += putMedian(termId, w, all, prev, engine, sourceTime);
            // COUNSELOR
            Map<Long, Agg> byCounselor = new HashMap<>();
            for (Map.Entry<Long, Agg> e : classes.entrySet()) {
                Long cid = classCounselor.get(e.getKey());
                if (cid == null) {
                    continue;
                }
                Agg target = byCounselor.computeIfAbsent(cid, k -> new Agg());
                merge(target, e.getValue());
            }
            for (Map.Entry<Long, Agg> e : byCounselor.entrySet()) {
                n += putKpi(termId, w, "COUNSELOR", e.getKey(), e.getValue(), prev, engine, sourceTime);
            }
        }
        return n;
    }

    private int putKpi(long termId, LocalDate week, String scopeType, long scopeId, Agg a,
                       Map<String, Double> prev, String engine, LocalDateTime sourceTime) {
        int n = 0;
        n += kpi(termId, week, scopeType, scopeId, "risk_ratio", a.riskRatio(), prev, engine, sourceTime);
        n += kpi(termId, week, scopeType, scopeId, "close_rate", a.closeRate(), prev, engine, sourceTime);
        n += kpi(termId, week, scopeType, scopeId, "avg_handle_hours", a.avgHours(), prev, engine, sourceTime);
        n += kpi(termId, week, scopeType, scopeId, "improve_rate", a.improveRate(), prev, engine, sourceTime);
        return n;
    }

    private int putMedian(long termId, LocalDate week, Agg all, Map<String, Double> prev,
                          String engine, LocalDateTime sourceTime) {
        Double median = jdbc.queryForObject(
                "select round(avg(hours),1) from (select timestampdiff(minute, create_time, close_time)/60.0 hours,"
                        + " row_number() over (order by timestampdiff(minute, create_time, close_time)) rn,"
                        + " count(*) over () n from alert_record where term_id = ? and is_void = 0"
                        + " and close_time >= ? and close_time < ?) x"
                        + " where rn in ((n+1) div 2, (n+2) div 2)",
                Double.class, termId, week, week.plusDays(7));
        return kpi(termId, week, "ALL", 0, "median_handle_hours", median, prev, engine, sourceTime);
    }

    private int kpi(long termId, LocalDate week, String scopeType, long scopeId, String metric,
                    Double current, Map<String, Double> prev, String engine, LocalDateTime sourceTime) {
        if (current == null || current.isNaN()) {
            return 0;
        }
        String key = scopeType + '|' + scopeId + '|' + metric;
        Double previous = prev.get(key);
        Double delta = previous == null ? null : round(current - previous);
        prev.put(key, current);
        jdbc.update("insert into stat_weekly_kpi (term_id, week_start, scope_type, scope_id, metric, current_value,"
                        + " prev_value, delta_rate, engine_version, source_max_update_time, is_stale)"
                        + " values (?,?,?,?,?,?,?,?,?,?,0)"
                        + " on duplicate key update current_value = values(current_value), prev_value = values(prev_value),"
                        + " delta_rate = values(delta_rate), engine_version = values(engine_version),"
                        + " source_max_update_time = values(source_max_update_time), is_stale = 0",
                termId, week, scopeType, scopeId, metric, current, previous, delta, engine, sourceTime);
        return 1;
    }

    private Agg rollup(Iterable<Agg> list) {
        Agg all = new Agg();
        for (Agg a : list) {
            merge(all, a);
        }
        return all;
    }

    private void merge(Agg target, Agg a) {
        target.hot += a.hot;
        target.total += a.total;
        target.created += a.created;
        target.closed += a.closed;
        target.hoursSum += a.hoursSum;
        target.closedCnt += a.closedCnt;
        target.due += a.due;
        target.improved += a.improved;
    }

    private Agg agg(Map<String, Map<Long, Agg>> byWeek, Map<String, Object> row, String wkKey, String classKey) {
        String wk = String.valueOf(row.get(wkKey));
        Long cid = ((Number) row.get(classKey)).longValue();
        return byWeek.computeIfAbsent(wk, k -> new LinkedHashMap<>())
                .computeIfAbsent(cid, k -> new Agg());
    }

    private List<LocalDate> weeklyStarts(long termId) {
        List<Object> dates = jdbc.queryForList(
                "select distinct stat_date from stat_daily_class where term_id = ? order by stat_date",
                Object.class, termId);
        Set<LocalDate> weeks = new LinkedHashSet<>();
        for (Object d : dates) {
            weeks.add(toDate(d).with(DayOfWeek.MONDAY));
        }
        return new ArrayList<>(weeks);
    }

    private Map<Long, Long> classCounselor(long termId) {
        Map<Long, Long> map = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(
                "select distinct e.teaching_class_id, cc.counselor_id from enrollment e"
                        + " join student st on st.id = e.student_id"
                        + " join counselor_class cc on cc.admin_class_id = st.admin_class_id and cc.term_id = ?",
                termId)) {
            map.put(((Number) r.get("teaching_class_id")).longValue(),
                    ((Number) r.get("counselor_id")).longValue());
        }
        return map;
    }

    // ============================================================ 干预成效
    private int refreshInterventionEffect(long termId, String engine, LocalDateTime sourceTime) {
        jdbc.update("delete from stat_intervention_effect where term_id = ?", termId);
        String valid = "ir.level_after is not null and ir.effect_check_time between ir.create_time + interval 7 day"
                + " and ir.effect_check_time <= ir.create_time + interval 14 day";
        String rankAfter = "(case ir.level_after when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)";
        String rankBefore = "(case ir.level_before when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)";
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select date_sub(date(ir.create_time), interval weekday(ir.create_time) day) week_start,"
                        + " ar.teaching_class_id, ir.cohort, count(*) intervened,"
                        + " sum(" + valid + ") due,"
                        + " sum(" + valid + " and " + rankAfter + " < " + rankBefore + ") improved,"
                        + " sum(" + valid + " and " + rankAfter + " = " + rankBefore + ") unchanged,"
                        + " sum(" + valid + " and " + rankAfter + " > " + rankBefore + ") worsened"
                        + " from intervention_record ir join alert_record ar on ar.id = ir.alert_id"
                        + " where ir.term_id = ? and ir.is_void = 0 and ir.cohort is not null"
                        + " group by week_start, ar.teaching_class_id, ir.cohort", termId);
        int n = 0;
        for (Map<String, Object> row : rows) {
            jdbc.update("insert into stat_intervention_effect (term_id, week_start, teaching_class_id, cohort,"
                            + " intervened, improved, unchanged, worsened, engine_version, source_max_update_time, is_stale)"
                            + " values (?,?,?,?,?,?,?,?,?,?,0)"
                            + " on duplicate key update intervened = values(intervened), improved = values(improved),"
                            + " unchanged = values(unchanged), worsened = values(worsened)",
                    termId, toDate(row.get("week_start")),
                    ((Number) row.get("teaching_class_id")).longValue(),
                    (String) row.get("cohort"),
                    ((Number) row.get("intervened")).intValue(),
                    ((Number) row.get("improved")).intValue(),
                    ((Number) row.get("unchanged")).intValue(),
                    ((Number) row.get("worsened")).intValue(),
                    engine, sourceTime);
            n++;
        }
        return n;
    }

    // ============================================================ 源数据/工具
    private Map<String, Object> currentSource(long termId) {
        return jdbc.queryForMap(
                "select coalesce(max(engine_version),'unknown') engine_version,"
                        + " greatest(coalesce(max(update_time), now()),"
                        + "   coalesce((select max(update_time) from alert_record where term_id = ?), now()),"
                        + "   coalesce((select max(update_time) from intervention_record where term_id = ?), now())) source_max_update_time"
                        + " from risk_state where term_id = ?", termId, termId, termId);
    }

    private static boolean isTrue(Object v) {
        return (v instanceof Boolean && (Boolean) v) || (v instanceof Number && ((Number) v).intValue() == 1);
    }

    private static double d(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? 0 : ((Number) v).doubleValue();
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static LocalDateTime toLdt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof LocalDateTime) {
            return (LocalDateTime) v;
        }
        if (v instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) v).toLocalDateTime();
        }
        return LocalDateTime.parse(String.valueOf(v).replace(' ', 'T').substring(0, 19));
    }

    private static LocalDate toDate(Object v) {
        if (v instanceof LocalDate) {
            return (LocalDate) v;
        }
        if (v instanceof java.sql.Date) {
            return ((java.sql.Date) v).toLocalDate();
        }
        if (v instanceof LocalDateTime) {
            return ((LocalDateTime) v).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(v).substring(0, 10));
    }
}
