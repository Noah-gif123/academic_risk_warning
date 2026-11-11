package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.vo.ai.ReportFact;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 学生 × 课程六维画像。只做聚合，不调用模型。
 *
 * <p>数据不足的维度返回 null，前端显示“—”，不按 0 分参与比较。</p>
 */
@Service
@RequiredArgsConstructor
public class StudentCourseMetricsService {

    private final JdbcTemplate jdbc;

    public Map<String, Object> compute(ReportFact fact) {
        List<Map<String, Object>> dims = new ArrayList<>();
        dims.add(dim("score", "总评成绩", fact.getTotalScore(), fact.getClassAvg(), "分"));
        dims.add(dim("attendance", "出勤率", attendance(fact), classAttendance(fact.getTeachingClassId()), "%"));
        dims.add(dim("homework", "作业完成率", homework(fact), classHomework(fact.getTeachingClassId()), "%"));
        dims.add(dim("riskControl", "风险控制", riskControl(fact.getEnrollmentId()), classRiskControl(fact.getTeachingClassId()), "分"));
        dims.add(dim("balance", "成绩均衡度", balance(fact), classBalance(fact.getTeachingClassId()), "分"));
        dims.add(dim("stability", "学习稳定性", stability(fact), null, "分"));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("dimensions", dims);
        out.put("teachingClassId", fact.getTeachingClassId());
        out.put("enrollmentId", fact.getEnrollmentId());
        out.put("caliber", "0–100，越高越好；数据不足为 null");
        out.put("snapshotAt", LocalDateTime.now().toString());
        return out;
    }

    private Map<String, Object> dim(String key, String label, Object student, Object classAvg, String unit) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        m.put("student", student);
        m.put("classAvg", classAvg);
        m.put("unit", unit);
        return m;
    }

    private BigDecimal attendance(ReportFact f) {
        if (f.getEffectiveSessions() == null || f.getEffectiveSessions() <= 0) return null;
        int absent = f.getAbsentCount() == null ? 0 : f.getAbsentCount();
        return clamp(100 - absent * 100.0 / f.getEffectiveSessions());
    }

    private BigDecimal classAttendance(Long teachingClassId) {
        try {
            Double v = jdbc.queryForObject("""
                    select avg(case when allowed_absent > 0
                        then greatest(0, 100 - absent_count * 100.0 / allowed_absent) end)
                    from risk_state
                    where teaching_class_id = ? and is_void = 0
                    """, Double.class, teachingClassId);
            return v == null ? null : BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal homework(ReportFact f) {
        List<ReportFact.Item> items = f.getItems() == null ? List.of() : f.getItems();
        if (items.isEmpty()) return null;
        long recorded = items.stream().filter(i -> !Boolean.TRUE.equals(i.getMissing())).count();
        return clamp(recorded * 100.0 / items.size());
    }

    private BigDecimal classHomework(Long teachingClassId) {
        try {
            Integer papers = jdbc.queryForObject(
                    "select count(*) from paper where teaching_class_id = ? and status <> 'DRAFT'",
                    Integer.class, teachingClassId);
            Integer students = jdbc.queryForObject(
                    "select count(*) from enrollment where teaching_class_id = ? and status in ('ACTIVE','ENROLLED')",
                    Integer.class, teachingClassId);
            Integer submissions = jdbc.queryForObject("""
                    select count(distinct s.paper_id, s.enrollment_id)
                    from submission s join enrollment e on e.id = s.enrollment_id
                    where e.teaching_class_id = ? and e.status in ('ACTIVE','ENROLLED')
                      and s.submit_status in ('SUBMITTED','MARKED','GRADED')
                    """, Integer.class, teachingClassId);
            if (papers == null || students == null || students == 0 || papers == 0) return null;
            return clamp(submissions == null ? 0 : submissions * 100.0 / (papers * students));
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal riskControl(Long enrollmentId) {
        try {
            BigDecimal risk = jdbc.queryForObject(
                    "select risk_score from risk_state where enrollment_id = ? and is_void = 0 limit 1",
                    BigDecimal.class, enrollmentId);
            return risk == null ? null : clamp(100 - risk.doubleValue());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal classRiskControl(Long teachingClassId) {
        try {
            Double avg = jdbc.queryForObject(
                    "select avg(risk_score) from risk_state where teaching_class_id = ? and is_void = 0",
                    Double.class, teachingClassId);
            return avg == null ? null : clamp(100 - avg);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal balance(ReportFact f) {
        List<BigDecimal> scores = f.getItems() == null ? List.of() : f.getItems().stream()
                .map(ReportFact.Item::getScore).filter(Objects::nonNull).toList();
        return stabilityOf(scores);
    }

    private BigDecimal classBalance(Long teachingClassId) {
        try {
            Double cv = jdbc.queryForObject("""
                    select avg(cv) from (
                        select student_id,
                               stddev_samp(total_score) / nullif(avg(total_score), 0) as cv
                        from submission
                        where teaching_class_id = ? and total_score is not null
                        group by student_id
                    ) t
                    """, Double.class, teachingClassId);
            return cv == null ? null : clamp(100 - Math.min(100, cv * 100));
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal stability(ReportFact f) {
        List<BigDecimal> rates = f.getWeeks() == null ? List.of() : f.getWeeks().stream()
                .map(ReportFact.Trend::getMyRate).filter(Objects::nonNull).toList();
        return stabilityOf(rates);
    }

    /** 100−变异系数；样本不足返回 null */
    private BigDecimal stabilityOf(List<BigDecimal> values) {
        if (values == null || values.size() < 2) return null;
        double avg = values.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        if (avg == 0) return BigDecimal.valueOf(100);
        double var = values.stream().mapToDouble(v -> Math.pow(v.doubleValue() - avg, 2)).sum() / (values.size() - 1);
        double cv = Math.sqrt(var) / avg;
        return clamp(100 - Math.min(100, cv * 100));
    }

    private BigDecimal clamp(double v) {
        return BigDecimal.valueOf(Math.max(0, Math.min(100, v))).setScale(1, RoundingMode.HALF_UP);
    }
}
