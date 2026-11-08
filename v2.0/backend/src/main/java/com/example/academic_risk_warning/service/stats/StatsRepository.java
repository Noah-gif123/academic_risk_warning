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
 * P1-4 统计只读查询（JdbcTemplate）。
 *
 * <p>P1-4 阶段物化表未落地，全部 SQL 直接读现有业务表：
 * risk_state / alert_record / intervention_record / score_result / teaching_class / course。
 * 每条指标 SQL 集中在本类，便于 P1-1 口径文档与 P1-8 对账复用。</p>
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
                + "from risk_state rs join student s on s.id = rs.student_id "
                + "where rs.term_id = ? and rs.is_void = 0 " + scopeFilter(scope, "s");
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
                + "from alert_record ar join student s on s.id = ar.student_id "
                + "where ar.term_id = ? and ar.is_void = 0 " + scopeFilter(scope, "s");
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
                + "from intervention_record ir join student s on s.id = ir.student_id "
                + "where ir.term_id = ? and ir.is_void = 0 and ir.create_time between ? and ? "
                + scopeFilter(scope, "s");
        return jdbc.queryForMap(sql, args.toArray());
    }

    // ============================================================ trend
    public List<Map<String, Object>> alertCreatedByWeek(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = trendArgs(scope, start, end);
        String sql = "select date_format(date_sub(ar.create_time, interval weekday(ar.create_time) day), '%Y-%m-%d') wk, "
                + "count(*) c from alert_record ar join student s on s.id = ar.student_id "
                + "where ar.term_id = ? and ar.is_void = 0 and ar.create_time between ? and ? "
                + scopeFilter(scope, "s") + " group by wk";
        return jdbc.queryForList(sql, args.toArray());
    }

    public List<Map<String, Object>> alertClosedByWeek(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = trendArgs(scope, start, end);
        String sql = "select date_format(date_sub(ar.close_time, interval weekday(ar.close_time) day), '%Y-%m-%d') wk, "
                + "count(*) c from alert_record ar join student s on s.id = ar.student_id "
                + "where ar.term_id = ? and ar.is_void = 0 and ar.close_time between ? and ? "
                + scopeFilter(scope, "s") + " group by wk";
        return jdbc.queryForList(sql, args.toArray());
    }

    public List<Map<String, Object>> interventionsByWeek(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = trendArgs(scope, start, end);
        String sql = "select date_format(date_sub(ir.create_time, interval weekday(ir.create_time) day), '%Y-%m-%d') wk, "
                + "count(*) c from intervention_record ir join student s on s.id = ir.student_id "
                + "where ir.term_id = ? and ir.is_void = 0 and ir.create_time between ? and ? "
                + scopeFilter(scope, "s") + " group by wk";
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
                + "count(*) total from risk_state rs join student s on s.id = rs.student_id "
                + "join course_offering co on co.id = rs.offering_id join course c on c.id = co.course_id "
                + "where rs.term_id = ? and rs.is_void = 0 " + scopeFilter(scope, "s")
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
                + "count(*) total from risk_state rs join student s on s.id = rs.student_id "
                + "join teaching_class tc on tc.id = rs.teaching_class_id "
                + "where rs.term_id = ? and rs.is_void = 0 " + scopeFilter(scope, "s")
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
                + "from risk_state rs join student s on s.id = rs.student_id "
                + "join teaching_class tc on tc.id = rs.teaching_class_id "
                + "join course_offering co on co.id = rs.offering_id join course c on c.id = co.course_id "
                + "left join score_result sr on sr.enrollment_id = rs.enrollment_id and sr.is_void = 0 "
                + "where rs.term_id = ? and rs.is_void = 0 " + scopeFilter(scope, "s")
                + " group by tc.id, tc.class_code, tc.class_name, c.course_name order by tc.class_code";
        return jdbc.queryForList(sql, args.toArray());
    }

    // ============================================================ funnel
    /** 周期内新增预警的处置漏斗（同批口径）。 */
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
                + "from alert_record ar join student s on s.id = ar.student_id "
                + "where ar.term_id = ? and ar.is_void = 0 and ar.create_time between ? and ? "
                + scopeFilter(scope, "s");
        return jdbc.queryForMap(sql, args.toArray());
    }

    // ============================================================ effect
    public Map<String, Object> effectAggregate(Scope scope, LocalDateTime start, LocalDateTime end) {
        List<Object> args = new ArrayList<>();
        args.add(scope.termId());
        args.add(start);
        args.add(end);
        addScopeArgs(args, scope);
        String sql = "select count(*) total, "
                + "coalesce(sum(ir.effect_check_time is not null and ir.effect_check_time <= now()), 0) due, "
                + "coalesce(sum(ir.effective = 'EFFECTIVE'), 0) improved, "
                + "coalesce(sum(ir.effective = 'INEFFECTIVE'), 0) unchanged, "
                + "coalesce(sum(ir.effective is null or ir.effective = 'PENDING'), 0) pending "
                + "from intervention_record ir join student s on s.id = ir.student_id "
                + "where ir.term_id = ? and ir.is_void = 0 and ir.create_time between ? and ? "
                + scopeFilter(scope, "s");
        return jdbc.queryForMap(sql, args.toArray());
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

    /** 辅导员范围过滤：管理员返回空串；空班名单返回恒假，避免 IN ()。 */
    private String scopeFilter(Scope scope, String alias) {
        if (scope.allScope()) {
            return "";
        }
        if (scope.classIds().isEmpty()) {
            return " and 1 = 0 ";
        }
        StringBuilder sb = new StringBuilder(" and ").append(alias).append(".admin_class_id in (");
        for (int i = 0; i < scope.classIds().size(); i++) {
            sb.append(i == 0 ? "?" : ", ?");
        }
        return sb.append(") ").toString();
    }

    private void addScopeArgs(List<Object> args, Scope scope) {
        if (!scope.allScope() && !scope.classIds().isEmpty()) {
            args.addAll(scope.classIds());
        }
    }
}
