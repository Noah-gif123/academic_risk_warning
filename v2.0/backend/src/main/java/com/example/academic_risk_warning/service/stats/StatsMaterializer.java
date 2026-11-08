package com.example.academic_risk_warning.service.stats;

import com.example.academic_risk_warning.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * P1-3 统计物化任务与失效校验。
 *
 * <p>职责：
 * <ol>
 *   <li>日快照：把当天 risk_state + 事件计数 upsert 进 stat_daily_class；</li>
 *   <li>周 KPI：由 stat_daily_class 与业务事件计算 close_rate/avg_handle_hours/improve_rate/risk_ratio，
 *       写 stat_weekly_kpi（scope ALL/0）；</li>
 *   <li>干预成效：按周 × 教学班 × cohort 计算改善/持平/恶化，写 stat_intervention_effect；</li>
 *   <li>失效校验：engine_version 或 source_max_update_time 落后即重算并清 is_stale；</li>
 *   <li>每日定时 + 手动重算（/api/stats/recompute，仅教务）。</li>
 * </ol>
 * 幂等：周 KPI/成效按周期先删后插，日快照按唯一键 upsert。</p>
 */
@Service
@RequiredArgsConstructor
public class StatsMaterializer {

    private final JdbcTemplate jdbc;

    /** 每日 02:10 刷新当前学期。 */
    @Scheduled(cron = "0 10 2 * * ?")
    public void scheduledRefresh() {
        try {
            Long termId = jdbc.queryForObject(
                    "select id from term where is_current = 1 order by id desc limit 1", Long.class);
            if (termId != null) {
                refresh(termId);
            }
        } catch (Exception ignored) {
            // 定时任务失败不阻塞应用；下次读取/手动重算会补
        }
    }

    /** 读取前保证新鲜：过期/为空/版本不一致则同步重算。 */
    public String ensureFresh(long termId) {
        if (needsRefresh(termId)) {
            return refresh(termId);
        }
        return "fresh";
    }

    public boolean needsRefresh(long termId) {
        Map<String, Object> cur = currentSource(termId);
        Integer rows = jdbc.queryForObject(
                "select count(*) from stat_daily_class", Integer.class);
        if (rows == null || rows == 0) {
            return true;
        }
        Map<String, Object> last = jdbc.queryForMap(
                "select engine_version, source_max_update_time, is_stale"
                        + " from stat_daily_class order by stat_date desc, id desc limit 1");
        Object stale = last.get("is_stale");
        if (stale != null) {
            if (stale instanceof Boolean && (Boolean) stale) {
                return true;
            }
            if (stale instanceof Number && ((Number) stale).intValue() == 1) {
                return true;
            }
        }
        String statEngine = (String) last.get("engine_version");
        if (statEngine == null || !statEngine.equals(cur.get("engine_version"))) {
            return true;
        }
        java.sql.Timestamp statTime = ts(last.get("source_max_update_time"));
        java.sql.Timestamp curTime = ts(cur.get("source_max_update_time"));
        return statTime == null || (curTime != null && curTime.after(statTime));
    }

    /** 全量重算并返回摘要。 */
    @Transactional
    public String refresh(long termId) {
        Map<String, Object> src = currentSource(termId);
        String engine = (String) src.get("engine_version");
        java.sql.Timestamp sourceTime = ts(src.get("source_max_update_time"));
        LocalDate today = LocalDate.now();
        int daily = upsertDaily(termId, today);
        int weeks = refreshWeeklyKpi(termId, engine, sourceTime);
        int effects = refreshInterventionEffect(termId, engine, sourceTime);
        jdbc.update("update stat_daily_class set engine_version = ?, source_max_update_time = ?, is_stale = 0",
                engine, sourceTime);
        jdbc.update("update stat_weekly_kpi set engine_version = ?, source_max_update_time = ?, is_stale = 0",
                engine, sourceTime);
        jdbc.update("update stat_intervention_effect set engine_version = ?, source_max_update_time = ?, is_stale = 0",
                engine, sourceTime);
        return "daily=" + daily + ", weeks=" + weeks + ", effects=" + effects;
    }

    // ============================================================ 日快照
    private int upsertDaily(long termId, LocalDate today) {
        jdbc.update("INSERT INTO stat_daily_class (stat_date, teaching_class_id, student_count, red, orange, yellow, green,"
                        + " avg_score, alert_new, alert_closed, intervention_count, engine_version,"
                        + " source_max_update_time, is_stale, create_time, update_time)"
                        + " SELECT ?, rs.teaching_class_id, COUNT(*),"
                        + " COALESCE(SUM(rs.risk_level='RED'),0), COALESCE(SUM(rs.risk_level='ORANGE'),0),"
                        + " COALESCE(SUM(rs.risk_level='YELLOW'),0), COALESCE(SUM(rs.risk_level='GREEN'),0),"
                        + " ROUND(AVG(rs.risk_score),2),"
                        + " (SELECT COUNT(*) FROM alert_record ar WHERE ar.teaching_class_id = rs.teaching_class_id"
                        + "   AND ar.term_id = rs.term_id AND ar.is_void = 0 AND DATE(ar.create_time) = ?),"
                        + " (SELECT COUNT(*) FROM alert_record ar WHERE ar.teaching_class_id = rs.teaching_class_id"
                        + "   AND ar.term_id = rs.term_id AND ar.is_void = 0 AND DATE(ar.close_time) = ?),"
                        + " (SELECT COUNT(*) FROM intervention_record ir JOIN alert_record ar2 ON ar2.id = ir.alert_id"
                        + "   WHERE ar2.teaching_class_id = rs.teaching_class_id AND ar2.term_id = rs.term_id"
                        + "   AND ir.is_void = 0 AND DATE(ir.create_time) = ?), NULL, NULL, 0, NOW(), NOW()"
                        + " FROM risk_state rs WHERE rs.term_id = ? AND rs.is_void = 0 GROUP BY rs.teaching_class_id"
                        + " ON DUPLICATE KEY UPDATE student_count = VALUES(student_count), red = VALUES(red),"
                        + " orange = VALUES(orange), yellow = VALUES(yellow), green = VALUES(green),"
                        + " avg_score = VALUES(avg_score), alert_new = VALUES(alert_new),"
                        + " alert_closed = VALUES(alert_closed), intervention_count = VALUES(intervention_count),"
                        + " update_time = NOW()",
                today, today, today, today, termId);
        return jdbc.queryForObject("select count(*) from stat_daily_class where stat_date = ?", Integer.class, today);
    }

    // ============================================================ 周 KPI
    private int refreshWeeklyKpi(long termId, String engine, java.sql.Timestamp sourceTime) {
        jdbc.update("delete from stat_weekly_kpi where scope_type = 'ALL' and scope_id = 0");
        List<LocalDate> weekStarts = weeklyStarts();
        int n = 0;
        for (LocalDate weekStart : weekStarts) {
            LocalDate weekEnd = weekStart.plusDays(6);
            java.sql.Timestamp s = java.sql.Timestamp.valueOf(weekStart.atStartOfDay());
            java.sql.Timestamp e = java.sql.Timestamp.valueOf(weekEnd.plusDays(1).atStartOfDay());
            Double riskRatio = riskRatio(weekStart, weekEnd);
            upsertKpi(weekStart, "risk_ratio", riskRatio, riskRatio(weekStart.minusWeeks(1), weekStart.minusDays(1)), engine, sourceTime);
            Double closeRate = closeRate(termId, s, e);
            upsertKpi(weekStart, "close_rate", closeRate, closeRate(termId,
                    java.sql.Timestamp.valueOf(weekStart.minusWeeks(1).atStartOfDay()),
                    java.sql.Timestamp.valueOf(weekStart.atStartOfDay())), engine, sourceTime);
            Double avgHours = avgHandleHours(termId, s, e);
            upsertKpi(weekStart, "avg_handle_hours", avgHours, avgHandleHours(termId,
                    java.sql.Timestamp.valueOf(weekStart.minusWeeks(1).atStartOfDay()),
                    java.sql.Timestamp.valueOf(weekStart.atStartOfDay())), engine, sourceTime);
            Double improveRate = improveRate(termId, s, e);
            upsertKpi(weekStart, "improve_rate", improveRate, null, engine, sourceTime);
            n += 4;
        }
        return n;
    }

    private List<LocalDate> weeklyStarts() {
        List<Object> dates = jdbc.queryForList(
                "select distinct stat_date from stat_daily_class order by stat_date", Object.class);
        Set<LocalDate> weeks = new LinkedHashSet<>();
        for (Object d : dates) {
            weeks.add(toDate(d).with(java.time.DayOfWeek.MONDAY));
        }
        return new java.util.ArrayList<>(weeks);
    }

    private Double riskRatio(LocalDate from, LocalDate to) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select sum(red + orange + yellow) hot, sum(student_count) total from stat_daily_class"
                        + " where stat_date between ? and ?", from, to);
        if (rows.isEmpty()) {
            return null;
        }
        Number hot = (Number) rows.get(0).get("hot");
        Number total = (Number) rows.get(0).get("total");
        return hot == null || total == null || total.doubleValue() == 0 ? null
                : round(hot.doubleValue() * 100.0 / total.doubleValue());
    }

    private Double closeRate(long termId, java.sql.Timestamp s, java.sql.Timestamp e) {
        Map<String, Object> row = jdbc.queryForMap(
                "select count(*) total, sum(close_time is not null) closed from alert_record"
                        + " where term_id = ? and is_void = 0 and create_time >= ? and create_time < ?",
                termId, s, e);
        Number total = (Number) row.get("total");
        Number closed = (Number) row.get("closed");
        return total == null || total.doubleValue() == 0 ? null
                : round(closed.doubleValue() * 100.0 / total.doubleValue());
    }

    private Double avgHandleHours(long termId, java.sql.Timestamp s, java.sql.Timestamp e) {
        Double v = jdbc.queryForObject(
                "select avg(timestampdiff(minute, create_time, close_time)) / 60 from alert_record"
                        + " where term_id = ? and is_void = 0 and close_time >= ? and close_time < ?",
                Double.class, termId, s, e);
        return v == null ? null : round(v);
    }

    private Double improveRate(long termId, java.sql.Timestamp s, java.sql.Timestamp e) {
        Map<String, Object> row = jdbc.queryForMap(
                "select count(*) due,"
                        + " sum(level_after is not null and effect_check_time between create_time + interval 7 day"
                        + "     and create_time + interval 14 day) valid,"
                        + " sum(level_after is not null and effect_check_time between create_time + interval 7 day"
                        + "     and create_time + interval 14 day and"
                        + "     (case level_after when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)"
                        + "     < (case level_before when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)) improved"
                        + " from intervention_record where term_id = ? and is_void = 0"
                        + " and create_time >= ? and create_time < ?", termId, s, e);
        Number valid = (Number) row.get("valid");
        Number improved = (Number) row.get("improved");
        return valid == null || valid.doubleValue() == 0 ? null
                : round(improved.doubleValue() * 100.0 / valid.doubleValue());
    }

    private void upsertKpi(LocalDate weekStart, String metric, Double current, Double prev,
                           String engine, java.sql.Timestamp sourceTime) {
        if (current == null) {
            return;
        }
        Double delta = prev == null || prev == 0 ? null : round(current - prev);
        jdbc.update("insert into stat_weekly_kpi (week_start, scope_type, scope_id, metric, current_value,"
                        + " prev_value, delta_rate, engine_version, source_max_update_time, is_stale)"
                        + " values (?, 'ALL', 0, ?, ?, ?, ?, ?, ?, 0)"
                        + " on duplicate key update current_value = values(current_value), prev_value = values(prev_value),"
                        + " delta_rate = values(delta_rate), engine_version = values(engine_version),"
                        + " source_max_update_time = values(source_max_update_time), is_stale = 0",
                weekStart, metric, current, prev, delta, engine, sourceTime);
    }

    // ============================================================ 干预成效
    private int refreshInterventionEffect(long termId, String engine, java.sql.Timestamp sourceTime) {
        jdbc.update("delete from stat_intervention_effect");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select date_sub(date(ir.create_time), interval weekday(ir.create_time) day) week_start,"
                        + " ar.teaching_class_id, ir.cohort, count(*) intervened,"
                        + " sum(ir.level_after is not null and ir.effect_check_time between ir.create_time + interval 7 day"
                        + "     and ir.create_time + interval 14 day) due,"
                        + " sum(ir.level_after is not null and ir.effect_check_time between ir.create_time + interval 7 day"
                        + "     and ir.create_time + interval 14 day and"
                        + "     (case ir.level_after when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)"
                        + "     < (case ir.level_before when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)) improved,"
                        + " sum(ir.level_after is not null and ir.effect_check_time between ir.create_time + interval 7 day"
                        + "     and ir.create_time + interval 14 day and"
                        + "     (case ir.level_after when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)"
                        + "     = (case ir.level_before when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)) unchanged,"
                        + " sum(ir.level_after is not null and ir.effect_check_time between ir.create_time + interval 7 day"
                        + "     and ir.create_time + interval 14 day and"
                        + "     (case ir.level_after when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)"
                        + "     > (case ir.level_before when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)) worsened"
                        + " from intervention_record ir join alert_record ar on ar.id = ir.alert_id"
                        + " where ir.term_id = ? and ir.is_void = 0 and ir.cohort is not null"
                        + " group by week_start, ar.teaching_class_id, ir.cohort", termId);
        int n = 0;
        for (Map<String, Object> row : rows) {
            jdbc.update("insert into stat_intervention_effect (week_start, teaching_class_id, cohort, intervened,"
                            + " improved, unchanged, worsened, engine_version, source_max_update_time, is_stale)"
                            + " values (?,?,?,?,?,?,?,?,?,0)"
                            + " on duplicate key update intervened = values(intervened), improved = values(improved),"
                            + " unchanged = values(unchanged), worsened = values(worsened)",
                    toDate(row.get("week_start")),
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

    private Map<String, Object> currentSource(long termId) {
        Map<String, Object> row = jdbc.queryForMap(
                "select coalesce(max(engine_version),'unknown') engine_version,"
                        + " greatest(coalesce(max(update_time), now()),"
                        + "   coalesce((select max(update_time) from alert_record where term_id = ?), now()),"
                        + "   coalesce((select max(update_time) from intervention_record where term_id = ?), now())) source_max_update_time"
                        + " from risk_state where term_id = ?", termId, termId, termId);
        return row;
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static java.sql.Timestamp ts(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof java.sql.Timestamp) {
            return (java.sql.Timestamp) v;
        }
        if (v instanceof java.time.LocalDateTime) {
            return java.sql.Timestamp.valueOf((java.time.LocalDateTime) v);
        }
        return java.sql.Timestamp.valueOf(String.valueOf(v).replace('T', ' '));
    }

    private static LocalDate toDate(Object v) {
        if (v instanceof LocalDate) {
            return (LocalDate) v;
        }
        if (v instanceof java.sql.Date) {
            return ((java.sql.Date) v).toLocalDate();
        }
        if (v instanceof java.time.LocalDateTime) {
            return ((java.time.LocalDateTime) v).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(v).substring(0, 10));
    }

    /** 供 controller 校验参数。 */
    public void requireTerm(Long termId) {
        if (termId == null) {
            throw new BizException(400, "termId 不能为空");
        }
    }
}
