package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.vo.ai.InterventionMetricsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 干预闭环度量（M10）：预警 → 干预 → 回访 → 效果。
 *
 * <p>四个指标：</p>
 * <ol>
 *   <li>**干预覆盖率** = 有干预记录的预警 / 预警总数；</li>
 *   <li>**回访有效率（转绿率）** = 回访结论为 EFFECTIVE / 已完成回访数；</li>
 *   <li>**平均响应时长** = 预警创建到首次干预的平均小时数；</li>
 *   <li>**复发率** = 干预后又产生新预警的比例。</li>
 * </ol>
 * <p>数字全部由 SQL 聚合，结论文案由模板拼装，可逐项复算。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterventionMetricsService {

    private final JdbcTemplate jdbc;

    public InterventionMetricsVO metrics(Long termId, Long counselorId) {
        Long term = termId;
        if (term == null) {
            term = jdbc.query("select id from term where is_current = 1 order by id desc limit 1", rs -> {
                return rs.next() ? rs.getLong(1) : null;
            });
        }
        InterventionMetricsVO vo = new InterventionMetricsVO();
        vo.setTermId(term);
        if (term != null) {
            List<String> names = jdbc.queryForList("select term_name from term where id = ?", String.class, term);
            vo.setTermName(names.isEmpty() ? null : names.get(0));
        }

        // 范围过滤：辅导员只看自己带的行政班
        String scopeFilter = "";
        List<Object> args = new ArrayList<>();
        if (counselorId != null) {
            scopeFilter = " and s.admin_class_id in (select admin_class_id from counselor_class where counselor_id = ?) ";
            args.add(counselorId);
            List<String> cnames = jdbc.queryForList("select counselor_name from counselor where id = ?", String.class, counselorId);
            vo.setScopeText((cnames.isEmpty() ? "辅导员" : cnames.get(0) + " 辅导员") + "名下行政班");
        } else {
            vo.setScopeText("全校（当前学期）");
        }

        String alertWhere = " where ar.is_void = 0 and (? is null or ar.term_id = ?) " + scopeFilter;
        List<Object> baseArgs = new ArrayList<>();
        baseArgs.add(term);
        baseArgs.add(term);
        baseArgs.addAll(args);

        Integer totalAlerts = jdbc.queryForObject(
                "select count(*) from alert_record ar join student s on s.id = ar.student_id " + alertWhere,
                Integer.class, baseArgs.toArray());
        vo.setTotalAlerts(nz(totalAlerts));

        Integer covered = jdbc.queryForObject(
                "select count(distinct ar.id) from alert_record ar join student s on s.id = ar.student_id "
                        + " join intervention_record ir on ir.alert_id = ar.id and ir.is_void = 0 " + alertWhere,
                Integer.class, baseArgs.toArray());
        vo.setCoveredAlerts(nz(covered));

        Integer interventions = jdbc.queryForObject(
                "select count(*) from intervention_record ir join alert_record ar on ar.id = ir.alert_id "
                        + " join student s on s.id = ar.student_id where ir.is_void = 0 and (? is null or ir.term_id = ?) "
                        + scopeFilter, Integer.class, baseArgs.toArray());
        vo.setInterventions(nz(interventions));

        Map<String, Object> eff = jdbc.queryForMap(
                "select "
                        + " sum(case when ir.effective = 'EFFECTIVE' then 1 else 0 end) as effective, "
                        + " sum(case when ir.effective = 'INEFFECTIVE' then 1 else 0 end) as ineffective, "
                        + " sum(case when ir.effective is null or ir.effective = 'PENDING' then 1 else 0 end) as pending "
                        + "from intervention_record ir join alert_record ar on ar.id = ir.alert_id "
                        + " join student s on s.id = ar.student_id where ir.is_void = 0 and (? is null or ir.term_id = ?) "
                        + scopeFilter, baseArgs.toArray());
        int effective = nz(i(eff.get("effective")));
        int ineffective = nz(i(eff.get("ineffective")));
        int pending = nz(i(eff.get("pending")));
        vo.setEffectiveCount(effective);
        vo.setIneffectiveCount(ineffective);
        vo.setPendingCount(pending);
        int followed = effective + ineffective;
        vo.setFollowed(followed);

        vo.setCoverageRate(pct(vo.getCoveredAlerts(), vo.getTotalAlerts()));
        vo.setEffectiveRate(pct(effective, followed));

        // 平均响应时长：每条预警取最早一次干预
        BigDecimal avgHours = jdbc.query(
                "select avg(timestampdiff(MINUTE, ar.create_time, ir.create_time)) / 60 "
                        + "from alert_record ar join student s on s.id = ar.student_id "
                        + " join intervention_record ir on ir.alert_id = ar.id and ir.is_void = 0 "
                        + " join (select alert_id, min(create_time) as first_time from intervention_record where is_void = 0 group by alert_id) f "
                        + "   on f.alert_id = ir.alert_id and f.first_time = ir.create_time "
                        + alertWhere, rs -> rs.next() ? rs.getBigDecimal(1) : null, baseArgs.toArray());
        vo.setAvgResponseHours(avgHours == null ? null : avgHours.setScale(1, RoundingMode.HALF_UP));

        // 复发率：干预之后同一 enrollment 又产生新预警
        Integer recurrence = jdbc.queryForObject(
                "select count(distinct ar2.id) from alert_record ar2 join student s on s.id = ar2.student_id "
                        + " join intervention_record ir on ir.alert_id <> ar2.id and ir.is_void = 0 "
                        + " join alert_record ar1 on ar1.id = ir.alert_id "
                        + " where ar2.is_void = 0 and ar1.is_void = 0 and (? is null or ar2.term_id = ?) "
                        + " and ar2.enrollment_id = ar1.enrollment_id and ar2.create_time > ir.create_time " + scopeFilter,
                Integer.class, baseArgs.toArray());
        vo.setRecurrenceCount(nz(recurrence));
        vo.setRecurrenceRate(pct(vo.getRecurrenceCount(), Math.max(1, vo.getCoveredAlerts())));

        // 按等级拆分
        List<InterventionMetricsVO.ByLevel> byLevel = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList(
                "select ar.alert_level as lv, count(distinct ar.id) as alerts, "
                        + " count(distinct case when ir.effective in ('EFFECTIVE','INEFFECTIVE') then ir.alert_id end) as followed, "
                        + " count(distinct case when ir.effective = 'EFFECTIVE' then ir.alert_id end) as effective "
                        + "from alert_record ar join student s on s.id = ar.student_id "
                        + " left join intervention_record ir on ir.alert_id = ar.id and ir.is_void = 0 "
                        + alertWhere + " group by ar.alert_level order by FIELD(ar.alert_level,'RED','YELLOW','GREEN')",
                baseArgs.toArray())) {
            InterventionMetricsVO.ByLevel b = new InterventionMetricsVO.ByLevel();
            String lv = String.valueOf(row.get("lv"));
            b.setLevel(lv);
            b.setLevelText(levelText(lv));
            b.setAlerts(nz(i(row.get("alerts"))));
            b.setFollowed(nz(i(row.get("followed"))));
            b.setEffective(nz(i(row.get("effective"))));
            b.setEffectiveRate(pct(b.getEffective(), b.getFollowed()));
            byLevel.add(b);
        }
        vo.setByLevel(byLevel);

        vo.setConclusion(conclusion(vo));
        return vo;
    }

    private String conclusion(InterventionMetricsVO v) {
        StringBuilder sb = new StringBuilder();
        sb.append("本学期共 ").append(nzf(v.getTotalAlerts())).append(" 条预警，其中 ")
                .append(nzf(v.getCoveredAlerts())).append(" 条已有人介入（覆盖率 ")
                .append(num(v.getCoverageRate())).append("%）；");
        if (v.getFollowed() == null || v.getFollowed() == 0) {
            sb.append("目前还没有完成回访的干预记录，暂无法计算有效率。");
        } else {
            sb.append("已完成回访 ").append(v.getFollowed()).append(" 条，有效 ")
                    .append(nzf(v.getEffectiveCount())).append(" 条（有效率 ")
                    .append(num(v.getEffectiveRate())).append("%）");
            if (v.getIneffectiveCount() != null && v.getIneffectiveCount() > 0) {
                sb.append("，未改善 ").append(v.getIneffectiveCount()).append(" 条已上报教务");
            }
            sb.append("；");
        }
        if (v.getAvgResponseHours() != null) {
            sb.append("平均在预警后 ").append(num(v.getAvgResponseHours())).append(" 小时响应；");
        }
        sb.append("干预后复发 ").append(nzf(v.getRecurrenceCount())).append(" 条。");
        if (v.getPendingCount() != null && v.getPendingCount() > 0) {
            sb.append("另有 ").append(v.getPendingCount()).append(" 条干预待回访。");
        }
        return sb.toString();
    }

    private static String levelText(String level) {
        if ("RED".equals(level)) {
            return "红灯";
        }
        if ("YELLOW".equals(level)) {
            return "黄灯";
        }
        if ("GREEN".equals(level)) {
            return "绿灯";
        }
        return level;
    }

    private static BigDecimal pct(Integer part, Integer total) {
        if (part == null || total == null || total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static String nzf(Integer v) {
        return v == null ? "0" : String.valueOf(v);
    }

    private static Integer i(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String num(BigDecimal v) {
        if (v == null) {
            return "0";
        }
        return v.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
