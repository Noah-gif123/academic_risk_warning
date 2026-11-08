package com.example.academic_risk_warning.service.stats;

import com.example.academic_risk_warning.service.stats.StatsScopeResolver.Scope;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * P1 统计只读查询（JdbcTemplate，班级级口径）。
 *
 * <p>范围统一为“教学班”：辅导员通过 enrollment→student→admin_class 映射得到 teachingClassIds；
 * 管理员为全校（scope.allScope()）。所有业务表过滤均走 teaching_class_id，
 * 与 stat_daily_class 的班级级快照保持一致（修复 P1 限制 4）。
 * 业务 SQL 集中在此，供 P1-8 对账与 P1-1 口径文档复用。</p>
 */
@Repository
@RequiredArgsConstructor
public class StatsRepository {

    private final JdbcTemplate jdbc;

    // ============================================================ overview
    public Map<String, Object> riskAggregate(Scope scope) {
        List<Object> args = new ArrayList<>();
        args.add(scope.termId());
        addScopeArgs(args, scope);
        String sql = "select count(*) total, "
                + "coalesce(sum(rs.risk_level = 'RED'), 0) red, "
                + "coalesce(sum(rs.risk_level = 'ORANGE'), 0) orange, "
                + "coalesce(sum(rs.risk_level = 'YELLOW'), 0) yellow, "
                + "coalesce(sum(rs.risk_level = 'GREEN'), 0) green, "
                + "coalesce(avg(rs.risk_score), 0) avgRisk, "
                + "count(distinct rs.teaching_class_id) classCount "
                + "from risk_state rs where rs.term_id = ? and rs.is_void = 0 " + scopeFilter(scope, "rs");
        return jdbc.queryForMap(sql, args.toArray());
    }

    public Map<String, Object> alertAggregate(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = new ArrayList<>();
        args.add(start);
        args.add(end);
        args.add(start);
        args.add(end);
        args.add(scope.termId());
        addScopeArgs(args, scope);
        String sql = "select coalesce(sum(ar.status in ('ACTIVE','HANDLING')), 0) active, "
                + "coalesce(sum(ar.create_time between ? and ?), 0) newIn, "
                + "coalesce(sum(ar.close_time between ? and ?), 0) closedIn "
                + "from alert_record ar where ar.term_id = ? and ar.is_void = 0 " + scopeFilter(scope, "ar");
        return jdbc.queryForMap(sql, args.toArray());
    }

    public Map<String, Object> interventionAggregate(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = new ArrayList<>();
        args.add(scope.termId());
        args.add(start);
        args.add(end);
        addScopeArgs(args, scope);
        String sql = "select count(*) interventions, "
                + "coalesce(sum(ir.effective = 'EFFECTIVE'), 0) improved "
                + "from intervention_record ir join alert_record ar on ar.id = ir.alert_id"
                + " where ir.term_id = ? and ir.is_void = 0"
                + " and ir.create_time between ? and ? " + scopeFilter(scope, "ar");
        return jdbc.queryForMap(sql, args.toArray());
    }

    // ============================================================ trend
    public List<Map<String, Object>> alertCreatedByWeek(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = trendArgs(scope, start, end);
        String sql = "select date_format(date_sub(ar.create_time, interval weekday(ar.create_time) day), '%Y-%m-%d') wk, "
                + "count(*) c from alert_record ar where ar.term_id = ? and ar.is_void = 0"
                + " and ar.create_time between ? and ? " + scopeFilter(scope, "ar") + " group by wk";
        return jdbc.queryForList(sql, args.toArray());
    }

    public List<Map<String, Object>> alertClosedByWeek(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = trendArgs(scope, start, end);
        String sql = "select date_format(date_sub(ar.close_time, interval weekday(ar.close_time) day), '%Y-%m-%d') wk, "
                + "count(*) c from alert_record ar where ar.term_id = ? and ar.is_void = 0"
                + " and ar.close_time between ? and ? " + scopeFilter(scope, "ar") + " group by wk";
        return jdbc.queryForList(sql, args.toArray());
    }

    public List<Map<String, Object>> interventionsByWeek(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = trendArgs(scope, start, end);
        String sql = "select date_format(date_sub(ir.create_time, interval weekday(ir.create_time) day), '%Y-%m-%d') wk, "
                + "count(*) c from intervention_record ir join alert_record ar on ar.id = ir.alert_id"
                + " where ir.term_id = ? and ir.is_void = 0"
                + " and ir.create_time between ? and ? " + scopeFilter(scope, "ar") + " group by wk";
        return jdbc.queryForList(sql, args.toArray());
    }

    public List<Map<String, Object>> improvedByWeek(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = trendArgs(scope, start, end);
        String sql = "select date_format(date_sub(ir.effect_check_time, interval weekday(ir.effect_check_time) day), '%Y-%m-%d') wk, "
                + "count(*) c from intervention_record ir join alert_record ar on ar.id = ir.alert_id"
                + " where ir.term_id = ? and ir.is_void = 0"
                + " and ir.effective = 'EFFECTIVE' and ir.effect_check_time between ? and ? "
                + scopeFilter(scope, "ar") + " group by wk";
        return jdbc.queryForList(sql, args.toArray());
    }

    /** 单次查询得到每周“被 >=2 门课预警”的学生数（性能：替代 8 次子查询）。 */
    public List<Map<String, Object>> multiWarnByWeek(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = trendArgs(scope, start, end);
        String sql = "select wk, count(*) c from ("
                + " select date_format(date_sub(ar.create_time, interval weekday(ar.create_time) day), '%Y-%m-%d') wk,"
                + " ar.student_id from alert_record ar where ar.term_id = ? and ar.is_void = 0"
                + " and ar.create_time between ? and ? " + scopeFilter(scope, "ar")
                + " group by wk, ar.student_id having count(distinct ar.course_id) >= 2) x group by wk";
        return jdbc.queryForList(sql, args.toArray());
    }

    // ============================================================ distribution
    public List<Map<String, Object>> courseDistribution(Scope scope) {
        List<Object> args = new ArrayList<>();
        args.add(scope.termId());
        addScopeArgs(args, scope);
        String sql = "select c.id, c.course_name, "
                + "coalesce(sum(rs.risk_level = 'RED'), 0) red, coalesce(sum(rs.risk_level = 'ORANGE'), 0) orange, "
                + "coalesce(sum(rs.risk_level = 'YELLOW'), 0) yellow, coalesce(sum(rs.risk_level = 'GREEN'), 0) green, "
                + "count(*) total from risk_state rs "
                + "join course_offering co on co.id = rs.offering_id join course c on c.id = co.course_id "
                + "where rs.term_id = ? and rs.is_void = 0 " + scopeFilter(scope, "rs")
                + " group by c.id, c.course_name order by c.id";
        return jdbc.queryForList(sql, args.toArray());
    }

    public List<Map<String, Object>> classDistribution(Scope scope, int limit) {
        List<Object> args = new ArrayList<>();
        args.add(scope.termId());
        addScopeArgs(args, scope);
        args.add(limit);
        String sql = "select tc.class_code, tc.class_name, "
                + "coalesce(sum(rs.risk_level = 'RED'), 0) red, coalesce(sum(rs.risk_level = 'ORANGE'), 0) orange, "
                + "coalesce(sum(rs.risk_level = 'YELLOW'), 0) yellow, coalesce(sum(rs.risk_level = 'GREEN'), 0) green, "
                + "count(*) total from risk_state rs join teaching_class tc on tc.id = rs.teaching_class_id "
                + "where rs.term_id = ? and rs.is_void = 0 " + scopeFilter(scope, "rs")
                + " group by tc.id, tc.class_code, tc.class_name order by red desc limit ?";
        return jdbc.queryForList(sql, args.toArray());
    }

    // ============================================================ compare
    public List<Map<String, Object>> classCompare(Scope scope) {
        List<Object> args = new ArrayList<>();
        args.add(scope.termId());
        addScopeArgs(args, scope);
        String sql = "select tc.class_code, tc.class_name, c.course_name, "
                + "count(distinct rs.student_id) studentCount, "
                + "coalesce(sum(rs.risk_level = 'RED'), 0) red, coalesce(sum(rs.risk_level = 'ORANGE'), 0) orange, "
                + "coalesce(sum(rs.risk_level = 'YELLOW'), 0) yellow, coalesce(sum(rs.risk_level = 'GREEN'), 0) green, "
                + "coalesce(avg(rs.risk_score), 0) avgRisk, coalesce(avg(sr.total_score), 0) avgScore, "
                + "coalesce(sum(rs.hard_fail = 1), 0) hardFail "
                + "from risk_state rs join teaching_class tc on tc.id = rs.teaching_class_id "
                + "join course_offering co on co.id = rs.offering_id join course c on c.id = co.course_id "
                + "left join score_result sr on sr.enrollment_id = rs.enrollment_id and sr.is_void = 0 "
                + "where rs.term_id = ? and rs.is_void = 0 " + scopeFilter(scope, "rs")
                + " group by tc.id, tc.class_code, tc.class_name, c.course_name order by tc.class_code";
        return jdbc.queryForList(sql, args.toArray());
    }

    // ============================================================ funnel / effect
    public Map<String, Object> funnelAggregate(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = new ArrayList<>();
        args.add(scope.termId());
        args.add(start);
        args.add(end);
        addScopeArgs(args, scope);
        String sql = "select count(*) created, "
                + "coalesce(sum(ar.handle_time is not null), 0) handled, "
                + "coalesce(sum(ar.close_time is not null), 0) closed, "
                + "coalesce(sum(ar.escalated = 1), 0) escalated "
                + "from alert_record ar where ar.term_id = ? and ar.is_void = 0"
                + " and ar.create_time between ? and ? " + scopeFilter(scope, "ar");
        return jdbc.queryForMap(sql, args.toArray());
    }

    /** 干预成效（T+7 窗口 + 等级序比较；含逾期未复评数）。 */
    public List<Map<String, Object>> effectByCohort(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = new ArrayList<>();
        args.add(scope.termId());
        args.add(start);
        args.add(end);
        addScopeArgs(args, scope);
        String rankAfter = "(case ir.level_after when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)";
        String rankBefore = "(case ir.level_before when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)";
        String valid = "ir.level_after is not null and ir.effect_check_time between ir.create_time + interval 7 day"
                + " and ir.create_time + interval 14 day";
        String sql = "select ir.cohort, count(*) intervened,"
                + " coalesce(sum(" + valid + "), 0) due,"
                + " coalesce(sum(" + valid + " and " + rankAfter + " < " + rankBefore + "), 0) improved,"
                + " coalesce(sum(" + valid + " and " + rankAfter + " = " + rankBefore + "), 0) unchanged,"
                + " coalesce(sum(" + valid + " and " + rankAfter + " > " + rankBefore + "), 0) worsened,"
                + " coalesce(sum(ir.effect_check_time is null), 0) notReviewed,"
                + " coalesce(sum(ir.effect_check_time > ir.create_time + interval 14 day), 0) overdue"
                + " from intervention_record ir join alert_record ar on ar.id = ir.alert_id"
                + " where ir.term_id = ? and ir.is_void = 0"
                + " and ir.create_time between ? and ? " + scopeFilter(scope, "ar") + " group by ir.cohort";
        return jdbc.queryForList(sql, args.toArray());
    }

    // ============================================================ 看板补充
    /** 关闭预警的处置时长分档、均值与中位数。 */
    public Map<String, Object> handleStats(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = new ArrayList<>();
        args.add(scope.termId());
        args.add(start);
        args.add(end);
        addScopeArgs(args, scope);
        String sql = "select count(*) total,"
                + " coalesce(sum(timestampdiff(minute, ar.create_time, ar.close_time) < 1440),0) lt24,"
                + " coalesce(sum(timestampdiff(minute, ar.create_time, ar.close_time) between 1440 and 4320),0) h24to72,"
                + " coalesce(sum(timestampdiff(minute, ar.create_time, ar.close_time) > 4320),0) gt72,"
                + " coalesce(round(avg(timestampdiff(minute, ar.create_time, ar.close_time)) / 60, 1),0) avgHours"
                + " from alert_record ar where ar.term_id = ? and ar.is_void = 0"
                + " and ar.close_time between ? and ? " + scopeFilter(scope, "ar");
        return jdbc.queryForMap(sql, args.toArray());
    }

    /** 处置时长中位数（MySQL 8 窗口函数）。 */
    public Double medianHandleHours(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = new ArrayList<>();
        args.add(scope.termId());
        args.add(start);
        args.add(end);
        addScopeArgs(args, scope);
        String sql = "select round(avg(hours), 1) from ("
                + " select timestampdiff(minute, ar.create_time, ar.close_time) / 60 hours,"
                + " row_number() over (order by timestampdiff(minute, ar.create_time, ar.close_time)) rn,"
                + " count(*) over () n from alert_record ar"
                + " where ar.term_id = ? and ar.is_void = 0 and ar.close_time between ? and ? "
                + scopeFilter(scope, "ar") + ") x where rn in ((n + 1) div 2, (n + 2) div 2)";
        return jdbc.queryForObject(sql, Double.class, args.toArray());
    }

    /** 课程因子校准散点（挂科率 vs course_factor）。 */
    public List<Map<String, Object>> calibration(long termId) {
        String sql = "select c.id course_id, c.course_name, cfr.fail_rate, cfr.course_factor, cfr.is_initialized"
                + " from course_fail_rate_history cfr join course c on c.id = cfr.course_id"
                + " where cfr.term_id = ? order by c.id";
        return jdbc.queryForList(sql, termId);
    }

    // ============================================================ 物化读取
    /** 物化日快照按 stat_date 聚合（term + 教学班范围）。 */
    public List<Map<String, Object>> dailySeries(Scope scope) {
        List<Object> args = new ArrayList<>();
        args.add(scope.termId());
        addClassScopeArgs(args, scope);
        String sql = "select stat_date, sum(red) red, sum(orange) orange, sum(yellow) yellow, sum(green) green,"
                + " sum(student_count) total, round(avg(avg_score), 2) avg_score"
                + " from stat_daily_class where term_id = ? " + classScopeFilter(scope, "stat_daily_class")
                + " group by stat_date order by stat_date";
        return jdbc.queryForList(sql, args.toArray());
    }

    // ============================================================ helpers
    private List<Object> trendArgs(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = new ArrayList<>();
        args.add(scope.termId());
        args.add(start);
        args.add(end);
        addScopeArgs(args, scope);
        return args;
    }

    /** 班级级范围过滤：管理员空串；空教学班名单恒假。 */
    private String scopeFilter(Scope scope, String alias) {
        if (scope.allScope()) {
            return "";
        }
        if (scope.teachingClassIds().isEmpty()) {
            return " and 1 = 0 ";
        }
        StringBuilder sb = new StringBuilder(" and ").append(alias).append(".teaching_class_id in (");
        for (int i = 0; i < scope.teachingClassIds().size(); i++) {
            sb.append(i == 0 ? "?" : ", ?");
        }
        return sb.append(") ").toString();
    }

    private String classScopeFilter(Scope scope, String alias) {
        return scopeFilter(scope, alias);
    }

    private void addScopeArgs(List<Object> args, Scope scope) {
        if (!scope.allScope() && !scope.teachingClassIds().isEmpty()) {
            args.addAll(scope.teachingClassIds());
        }
    }

    private void addClassScopeArgs(List<Object> args, Scope scope) {
        addScopeArgs(args, scope);
    }

    /** 当前学期 ID。 */
    public Long currentTermId() {
        List<Long> ids = jdbc.queryForList(
                "select id from term where is_current = 1 order by id desc limit 1", Long.class);
        return ids.isEmpty() ? null : ids.get(0);
    }

    /** 当前引擎版本。 */
    public String currentEngine(long termId) {
        return jdbc.queryForObject(
                "select coalesce(max(engine_version), '') from risk_state where term_id = ?", String.class, termId);
    }
}
