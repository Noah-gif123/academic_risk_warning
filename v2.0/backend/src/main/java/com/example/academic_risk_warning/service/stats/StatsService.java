package com.example.academic_risk_warning.service.stats;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.service.stats.StatsScopeResolver.Scope;
import com.example.academic_risk_warning.vo.stats.StatsEnvelope;
import com.example.academic_risk_warning.vo.stats.StatsQuery;
import com.example.academic_risk_warning.vo.stats.StatsVos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P1-4 统计口径编排。
 *
 * <p>阶段约定（D5，已确认最小口径）：
 * <ul>
 *   <li>关闭率分母 = 周期内新增预警；处置时长 = close_time − create_time（小时）；</li>
 *   <li>干预改善暂用 intervention_record.effective='EFFECTIVE'（T+7 等级字段待 P1-0/P1-1 补齐）；</li>
 *   <li>P1-4 不做对照组（无 cohort 字段），effect 只返回干预组成效；</li>
 *   <li>P1-2/P1-3 物化表未落地：风险历史、trend 风险序列等以 PARTIAL/NO_DATA 显式降级。</li>
 * </ul></p>
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatsScopeResolver scopeResolver;
    private final StatsRepository repo;
    private final StatsMaterializer materializer;

    /** 统计区间：当前期 + 上期（等长）。 */
    private record Period(LocalDateTime start, LocalDateTime end,
                          LocalDateTime prevStart, LocalDateTime prevEnd) {
    }

    // ============================================================ 七接口
    public StatsEnvelope<StatsVos.Overview> overview(LoginUser user, StatsQuery query) {
        Scope scope = scopeResolver.resolve(user, query);
        materializer.ensureFresh(scope.termId());
        Period period = period(query, scope);
        Map<String, Object> risk = repo.riskAggregate(scope);
        Map<String, Object> alerts = repo.alertAggregate(scope, period.start(), period.end());
        Map<String, Object> interventions = repo.interventionAggregate(scope, period.start(), period.end());
        Map<String, Object> prevAlerts = repo.alertAggregate(scope, period.prevStart(), period.prevEnd());
        Map<String, Object> prevInterventions = repo.interventionAggregate(scope, period.prevStart(), period.prevEnd());

        StatsVos.Overview current = fillOverview(scope, risk, alerts, interventions);
        Map<String, Object> handle = repo.handleStats(scope, period.start(), period.end());
        current.setCloseRate(pct(i(alerts, "closedIn"), i(alerts, "newIn")));
        current.setAvgHandleHours(dec(handle, "avgHours"));
        StatsVos.HandleBuckets buckets = new StatsVos.HandleBuckets();
        buckets.setLt24(i(handle, "lt24"));
        buckets.setH24to72(i(handle, "h24to72"));
        buckets.setGt72(i(handle, "gt72"));
        buckets.setTotal(i(handle, "total"));
        current.setHandleBuckets(buckets);
        StatsVos.Overview previous = new StatsVos.Overview();
        previous.setTermName(scope.termName());
        previous.setAlertNew(i(prevAlerts, "newIn"));
        previous.setAlertClosed(i(prevAlerts, "closedIn"));
        previous.setInterventions(i(prevInterventions, "interventions"));
        previous.setImproved(i(prevInterventions, "improved"));

        BigDecimal currentRatio = riskRatio(i(risk, "red"), i(risk, "orange"), i(risk, "yellow"), i(risk, "total"));
        current.setRiskRatio(currentRatio);
        List<Map<String, Object>> daily = repo.dailySeries(scope);
        Map<LocalDate, Map<String, Object>> lastOfWeek = new LinkedHashMap<>();
        for (Map<String, Object> row : daily) {
            LocalDate d = toLocalDate(row.get("stat_date"));
            lastOfWeek.put(d.with(DayOfWeek.MONDAY), row);
        }
        List<Map<String, Object>> weeks = new ArrayList<>(lastOfWeek.values());
        BigDecimal delta = null;
        String reason = null;
        if (weeks.size() >= 2) {
            Map<String, Object> prev = weeks.get(weeks.size() - 2);
            previous.setRed(i(prev, "red"));
            previous.setOrange(i(prev, "orange"));
            previous.setYellow(i(prev, "yellow"));
            previous.setGreen(i(prev, "green"));
            previous.setTotal(i(prev, "total"));
            previous.setAvgRiskScore(dec(prev, "avg_score"));
            previous.setRiskRatio(riskRatio(i(prev, "red"), i(prev, "orange"), i(prev, "yellow"), i(prev, "total")));
            if (currentRatio != null && previous.getRiskRatio() != null) {
                delta = currentRatio.subtract(previous.getRiskRatio()).setScale(2, RoundingMode.HALF_UP);
            }
        } else {
            reason = "物化历史不足：仅当前快照，previous/deltaRate 留空（P1-3 物化后补齐）";
        }
        StatsEnvelope<StatsVos.Overview> envelope = reason == null
                ? StatsEnvelope.ok(current) : StatsEnvelope.partial(current, reason);
        envelope.setPrevious(previous);
        envelope.setDeltaRate(delta);
        envelope.setSampleSize(i(risk, "total"));
        fillMeta(envelope, scope, period);
        return envelope;
    }

    public StatsEnvelope<StatsVos.Trend> trend(LoginUser user, StatsQuery query) {
        Scope scope = scopeResolver.resolve(user, query);
        LocalDate thisMonday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate from = thisMonday.minusWeeks(7);
        if (scope.termStart() != null && scope.termStart().isAfter(from)) {
            from = scope.termStart();
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        Map<String, StatsVos.TrendPoint> points = new LinkedHashMap<>();
        List<LocalDate> weeks = new ArrayList<>();
        LocalDate cursor = from.with(DayOfWeek.MONDAY);
        while (!cursor.isAfter(thisMonday)) {
            weeks.add(cursor);
            cursor = cursor.plusWeeks(1);
        }
        for (LocalDate week : weeks) {
            StatsVos.TrendPoint p = new StatsVos.TrendPoint();
            p.setWeekStart(week.format(DATE));
            p.setAlertNew(0);
            p.setAlertClosed(0);
            p.setInterventions(0);
            p.setImproved(0);
            p.setMultiWarn(repo.multiWarnStudents(scope, week.atStartOfDay(), week.plusDays(7).atStartOfDay()));
            points.put(p.getWeekStart(), p);
        }
        mergeWeekly(points, repo.alertCreatedByWeek(scope, start, end), "alertNew");
        mergeWeekly(points, repo.alertClosedByWeek(scope, start, end), "alertClosed");
        mergeWeekly(points, repo.interventionsByWeek(scope, start, end), "interventions");
        mergeWeekly(points, repo.improvedByWeek(scope, start, end), "improved");
        for (Map<String, Object> row : repo.dailySeries(scope)) {
            LocalDate d = toLocalDate(row.get("stat_date"));
            StatsVos.TrendPoint p = points.get(d.with(DayOfWeek.MONDAY).format(DATE));
            if (p == null) {
                continue;
            }
            p.setRed(i(row, "red"));
            p.setYellow(i(row, "yellow"));
            p.setRiskRatio(riskRatio(i(row, "red"), i(row, "orange"), i(row, "yellow"), i(row, "total")));
        }

        StatsVos.Trend trend = new StatsVos.Trend();
        trend.setPoints(new ArrayList<>(points.values()));
        boolean anyRisk = points.values().stream().anyMatch(p -> p.getRiskRatio() != null);
        boolean anyEvent = points.values().stream().anyMatch(p -> p.getAlertNew() > 0 || p.getAlertClosed() > 0
                || p.getInterventions() > 0 || p.getImproved() > 0);
        StatsEnvelope<StatsVos.Trend> envelope;
        if (anyRisk) {
            envelope = StatsEnvelope.ok(trend);
        } else if (anyEvent) {
            envelope = StatsEnvelope.partial(trend, "风险历史快照不足：趋势仅含预警/干预事件口径");
        } else {
            envelope = StatsEnvelope.noData("周期内没有预警/干预事件");
        }
        fillMeta(envelope, scope, new Period(start, end, start.minusDays(7), start.minusSeconds(1)));
        return envelope;
    }

    public StatsEnvelope<StatsVos.Distribution> distribution(LoginUser user, StatsQuery query) {
        Scope scope = scopeResolver.resolve(user, query);
        Map<String, Object> risk = repo.riskAggregate(scope);
        StatsVos.Distribution d = new StatsVos.Distribution();
        d.getLevels().put("RED", i(risk, "red"));
        d.getLevels().put("ORANGE", i(risk, "orange"));
        d.getLevels().put("YELLOW", i(risk, "yellow"));
        d.getLevels().put("GREEN", i(risk, "green"));
        for (Map<String, Object> row : repo.courseDistribution(scope)) {
            StatsVos.DistRow r = new StatsVos.DistRow();
            r.setCode(String.valueOf(row.get("id")));
            r.setName(String.valueOf(row.get("course_name")));
            r.setRed(i(row, "red"));
            r.setOrange(i(row, "orange"));
            r.setYellow(i(row, "yellow"));
            r.setGreen(i(row, "green"));
            r.setTotal(i(row, "total"));
            d.getByCourse().add(r);
        }
        for (Map<String, Object> row : repo.classDistribution(scope, 30)) {
            StatsVos.DistRow r = new StatsVos.DistRow();
            r.setCode(String.valueOf(row.get("class_code")));
            r.setName(String.valueOf(row.get("class_name")));
            r.setRed(i(row, "red"));
            r.setOrange(i(row, "orange"));
            r.setYellow(i(row, "yellow"));
            r.setGreen(i(row, "green"));
            r.setTotal(i(row, "total"));
            d.getByClass().add(r);
        }
        for (Map<String, Object> row : repo.calibration(scope.termId())) {
            StatsVos.Calibration cal = new StatsVos.Calibration();
            cal.setCourseId(row.get("course_id") == null ? null : ((Number) row.get("course_id")).longValue());
            cal.setCourseName(String.valueOf(row.get("course_name")));
            cal.setFailRate(dec(row, "fail_rate"));
            cal.setCourseFactor(dec(row, "course_factor"));
            Object init = row.get("is_initialized");
            cal.setInitialized(init != null && ((init instanceof Boolean && (Boolean) init)
                    || (init instanceof Number && ((Number) init).intValue() == 1)));
            d.getCalibration().add(cal);
        }
        StatsEnvelope<StatsVos.Distribution> envelope = StatsEnvelope.ok(d);
        envelope.setSampleSize(i(risk, "total"));
        fillMeta(envelope, scope, period(query, scope));
        return envelope;
    }

    public StatsEnvelope<StatsVos.Compare> compare(LoginUser user, StatsQuery query) {
        Scope scope = scopeResolver.resolve(user, query);
        StatsVos.Compare c = new StatsVos.Compare();
        for (Map<String, Object> row : repo.classCompare(scope)) {
            StatsVos.CompareRow r = new StatsVos.CompareRow();
            r.setClassCode(String.valueOf(row.get("class_code")));
            r.setClassName(String.valueOf(row.get("class_name")));
            r.setCourseName(String.valueOf(row.get("course_name")));
            r.setStudentCount(i(row, "studentCount"));
            r.setRed(i(row, "red"));
            r.setOrange(i(row, "orange"));
            r.setYellow(i(row, "yellow"));
            r.setGreen(i(row, "green"));
            r.setAvgRiskScore(dec(row, "avgRisk"));
            r.setAvgScore(dec(row, "avgScore"));
            r.setHardFail(i(row, "hardFail"));
            c.getRows().add(r);
        }
        StatsEnvelope<StatsVos.Compare> envelope = StatsEnvelope.ok(c);
        envelope.setSampleSize(c.getRows().size());
        fillMeta(envelope, scope, period(query, scope));
        return envelope;
    }

    public StatsEnvelope<StatsVos.Funnel> funnel(LoginUser user, StatsQuery query) {
        Scope scope = scopeResolver.resolve(user, query);
        Period period = period(query, scope);
        Map<String, Object> f = repo.funnelAggregate(scope, period.start(), period.end());
        Map<String, Object> iv = repo.interventionAggregate(scope, period.start(), period.end());
        StatsVos.Funnel funnel = new StatsVos.Funnel();
        int created = i(f, "created");
        funnel.getStages().add(new StatsVos.FunnelStage("created", "周期内新增预警", created));
        funnel.getStages().add(new StatsVos.FunnelStage("handled", "已处置", i(f, "handled")));
        funnel.getStages().add(new StatsVos.FunnelStage("closed", "已关闭", i(f, "closed")));
        funnel.getStages().add(new StatsVos.FunnelStage("intervened", "已干预", i(iv, "interventions")));
        funnel.getStages().add(new StatsVos.FunnelStage("improved", "已改善", i(iv, "improved")));
        StatsEnvelope<StatsVos.Funnel> envelope;
        if (created == 0) {
            envelope = StatsEnvelope.noData("周期内无新增预警");
        } else if (i(f, "closed") == 0 && i(f, "handled") == 0 && i(iv, "interventions") == 0) {
            envelope = StatsEnvelope.partial(funnel,
                    "预警均为 ACTIVE，处置/关闭/干预样本为 0（演示生命周期待 P1-0 补齐）");
        } else {
            envelope = StatsEnvelope.ok(funnel);
        }
        envelope.setSampleSize(created);
        fillMeta(envelope, scope, period);
        return envelope;
    }

    public StatsEnvelope<StatsVos.Effect> effect(LoginUser user, StatsQuery query) {
        Scope scope = scopeResolver.resolve(user, query);
        materializer.ensureFresh(scope.termId());
        Period period = period(query, scope);
        List<Map<String, Object>> rows = repo.effectByCohort(scope, period.start(), period.end());
        StatsVos.Effect effect = new StatsVos.Effect();
        int total = 0, due = 0, improved = 0, unchanged = 0, worsened = 0;
        BigDecimal treatRate = null, controlRate = null;
        for (Map<String, Object> row : rows) {
            StatsVos.Cohort c = new StatsVos.Cohort();
            c.setCohort(String.valueOf(row.get("cohort")));
            c.setIntervened(i(row, "intervened"));
            c.setDue(i(row, "due"));
            c.setImproved(i(row, "improved"));
            c.setUnchanged(i(row, "unchanged"));
            c.setWorsened(i(row, "worsened"));
            c.setImproveRate(pct(i(row, "improved"), i(row, "due")));
            effect.getCohorts().add(c);
            total += c.getIntervened();
            due += c.getDue();
            improved += c.getImproved();
            unchanged += c.getUnchanged();
            worsened += c.getWorsened();
            if ("TREAT".equals(c.getCohort())) {
                treatRate = c.getImproveRate();
            } else if ("CONTROL".equals(c.getCohort())) {
                controlRate = c.getImproveRate();
            }
        }
        effect.setInterventions(total);
        effect.setDue(due);
        effect.setImproved(improved);
        effect.setUnchanged(unchanged);
        effect.setWorsened(worsened);
        effect.setPending(Math.max(0, total - due));
        effect.setImproveRate(pct(improved, due));
        effect.setTreatRate(treatRate);
        effect.setControlRate(controlRate);
        effect.setDiffPp(treatRate != null && controlRate != null
                ? treatRate.subtract(controlRate).setScale(2, RoundingMode.HALF_UP) : null);
        effect.setNote("口径：T+7（第 7–14 天）复评等级下降算改善；CONTROL 为延迟干预对照组（准实验）");
        StatsEnvelope<StatsVos.Effect> envelope;
        if (total == 0) {
            envelope = StatsEnvelope.noData("周期内没有干预记录");
        } else if (due == 0) {
            envelope = StatsEnvelope.partial(effect, "干预尚未到 T+7 复评窗口");
        } else {
            envelope = StatsEnvelope.ok(effect);
        }
        envelope.setSampleSize(total);
        fillMeta(envelope, scope, period);
        return envelope;
    }

    /** 导出 CSV（P1-4 先支持 overview/distribution/compare）。 */
    public String exportCsv(LoginUser user, StatsQuery query, String type) {
        StringBuilder sb = new StringBuilder();
        String t = type == null ? "overview" : type.trim().toLowerCase();
        switch (t) {
            case "distribution": {
                StatsEnvelope<StatsVos.Distribution> e = distribution(user, query);
                sb.append("等级,数量\n");
                e.getCurrent().getLevels().forEach((k, v) -> sb.append(k).append(',').append(v).append('\n'));
                sb.append("\n课程ID,课程,红,橙,黄,绿,合计\n");
                for (StatsVos.DistRow r : e.getCurrent().getByCourse()) {
                    sb.append(csv(r.getCode())).append(',').append(csv(r.getName())).append(',')
                            .append(r.getRed()).append(',').append(r.getOrange()).append(',')
                            .append(r.getYellow()).append(',').append(r.getGreen()).append(',')
                            .append(r.getTotal()).append('\n');
                }
                break;
            }
            case "compare": {
                StatsEnvelope<StatsVos.Compare> e = compare(user, query);
                sb.append("教学班,班级,课程,人数,红,橙,黄,绿,平均风险分,平均分,缺课硬线\n");
                for (StatsVos.CompareRow r : e.getCurrent().getRows()) {
                    sb.append(csv(r.getClassCode())).append(',').append(csv(r.getClassName())).append(',')
                            .append(csv(r.getCourseName())).append(',').append(r.getStudentCount()).append(',')
                            .append(r.getRed()).append(',').append(r.getOrange()).append(',')
                            .append(r.getYellow()).append(',').append(r.getGreen()).append(',')
                            .append(r.getAvgRiskScore()).append(',').append(r.getAvgScore()).append(',')
                            .append(r.getHardFail()).append('\n');
                }
                break;
            }
            default: {
                StatsEnvelope<StatsVos.Overview> e = overview(user, query);
                StatsVos.Overview o = e.getCurrent();
                sb.append("指标,数值\n");
                sb.append("学期,").append(csv(o.getTermName())).append('\n');
                sb.append("风险总人数,").append(o.getTotal()).append('\n');
                sb.append("红,").append(o.getRed()).append('\n');
                sb.append("橙,").append(o.getOrange()).append('\n');
                sb.append("黄,").append(o.getYellow()).append('\n');
                sb.append("绿,").append(o.getGreen()).append('\n');
                sb.append("平均风险分,").append(o.getAvgRiskScore()).append('\n');
                sb.append("活跃预警,").append(o.getActiveAlerts()).append('\n');
                sb.append("周期新增预警,").append(o.getAlertNew()).append('\n');
                sb.append("周期关闭预警,").append(o.getAlertClosed()).append('\n');
                sb.append("周期干预,").append(o.getInterventions()).append('\n');
                sb.append("数据状态,").append(e.getDataStatus()).append('\n');
                break;
            }
        }
        return sb.toString();
    }

    // ============================================================ 周期与工具
    private Period period(StatsQuery query, Scope scope) {
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;
        if (notBlank(query.getStartDate()) && notBlank(query.getEndDate())) {
            try {
                start = LocalDate.parse(query.getStartDate(), DATE);
                end = LocalDate.parse(query.getEndDate(), DATE);
            } catch (DateTimeParseException ex) {
                throw new BizException(400, "日期格式应为 yyyy-MM-dd");
            }
            if (end.isBefore(start)) {
                throw new BizException(400, "endDate 不能早于 startDate");
            }
        } else {
            String p = query.getPeriod() == null ? "week" : query.getPeriod().trim().toLowerCase();
            switch (p) {
                case "week":
                    start = today.with(DayOfWeek.MONDAY);
                    end = today;
                    break;
                case "month":
                    start = today.withDayOfMonth(1);
                    end = today;
                    break;
                case "term":
                    start = scope.termStart() == null ? today.withDayOfMonth(1) : scope.termStart();
                    end = today;
                    break;
                default:
                    throw new BizException(400, "period 仅支持 week/month/term");
            }
        }
        LocalDateTime s = start.atStartOfDay();
        LocalDateTime e = end.plusDays(1).atStartOfDay().minusSeconds(1);
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate ps = start.minusDays(days);
        LocalDate pe = start.minusDays(1);
        return new Period(s, e, ps.atStartOfDay(), pe.plusDays(1).atStartOfDay().minusSeconds(1));
    }

    private StatsVos.Overview fillOverview(Scope scope, Map<String, Object> risk,
                                           Map<String, Object> alerts, Map<String, Object> interventions) {
        StatsVos.Overview o = new StatsVos.Overview();
        o.setTermName(scope.termName());
        o.setClassCount(i(risk, "classCount"));
        o.setTotal(i(risk, "total"));
        o.setRed(i(risk, "red"));
        o.setOrange(i(risk, "orange"));
        o.setYellow(i(risk, "yellow"));
        o.setGreen(i(risk, "green"));
        o.setAvgRiskScore(dec(risk, "avgRisk"));
        o.setActiveAlerts(i(alerts, "active"));
        o.setAlertNew(i(alerts, "newIn"));
        o.setAlertClosed(i(alerts, "closedIn"));
        o.setInterventions(i(interventions, "interventions"));
        o.setImproved(i(interventions, "improved"));
        return o;
    }

    private void fillMeta(StatsEnvelope<?> envelope, Scope scope, Period period) {
        envelope.setAsOf(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        envelope.setPeriodStart(period.start().format(DATETIME));
        envelope.setPeriodEnd(period.end().format(DATETIME));
        envelope.setStale(Boolean.FALSE);
        envelope.setEngineVersion(repo.currentEngine(scope.termId()));
        envelope.setMaterializedAt(envelope.getAsOf());
    }

    /** 手动重算（P1-3，教务）。 */
    public String recompute(Long termId) {
        Long id = termId != null ? termId : repo.currentTermId();
        if (id == null) {
            throw new BizException(400, "没有当前学期");
        }
        return materializer.refresh(id);
    }

    private static BigDecimal riskRatio(int red, int orange, int yellow, int total) {
        if (total <= 0) {
            return null;
        }
        return BigDecimal.valueOf((red + orange + yellow) * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal pct(int n, int d) {
        return d <= 0 ? null : BigDecimal.valueOf(n * 100.0 / d).setScale(2, RoundingMode.HALF_UP);
    }

    private static LocalDate toLocalDate(Object v) {
        if (v == null) {
            return LocalDate.now();
        }
        if (v instanceof LocalDate) {
            return (LocalDate) v;
        }
        if (v instanceof java.sql.Date) {
            return ((java.sql.Date) v).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(v).substring(0, 10));
    }

    private void mergeWeekly(Map<String, StatsVos.TrendPoint> points, List<Map<String, Object>> rows,
                             String field) {
        for (Map<String, Object> row : rows) {
            String wk = String.valueOf(row.get("wk"));
            StatsVos.TrendPoint p = points.get(wk);
            if (p == null) {
                continue;
            }
            int value = i(row, "c");
            switch (field) {
                case "alertNew": p.setAlertNew(value); break;
                case "alertClosed": p.setAlertClosed(value); break;
                case "interventions": p.setInterventions(value); break;
                case "improved": p.setImproved(value); break;
                default: break;
            }
        }
    }

    private static BigDecimal deltaRate(int current, int previous) {
        if (previous == 0) {
            return null;
        }
        return BigDecimal.valueOf((current - previous) * 100.0 / previous).setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static int i(Map<String, Object> row, String key) {
        Object v = row == null ? null : row.get(key);
        return v == null ? 0 : ((Number) v).intValue();
    }

    private static BigDecimal dec(Map<String, Object> row, String key) {
        Object v = row == null ? null : row.get(key);
        if (v == null) {
            return null;
        }
        return new BigDecimal(String.valueOf(v)).setScale(2, RoundingMode.HALF_UP);
    }

    private static String csv(String v) {
        if (v == null) {
            return "";
        }
        return '"' + v.replace("\"", "\"\"") + '"';
    }
}
