package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.service.ScoreMatrixService;
import com.example.academic_risk_warning.vo.StudentScoreVO;
import com.example.academic_risk_warning.vo.ai.ReportFact;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 事实包构建：把"这个学生这门课现在到底什么状况"算清楚。
 *
 * <p>只用系统已有的四个维度：成绩构成项 / 试卷与周次 / 题目分组名 / 题型，
 * 加上考勤、项目贡献、预警结论。**不引入知识点**。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactPackBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbc;
    private final ScoreMatrixService scoreMatrixService;

    private static final String BASE_SQL = """
            select e.id            as enrollment_id,
                   e.student_id    as student_id,
                   e.teaching_class_id,
                   e.offering_id,
                   e.term_id,
                   c.id            as course_id,
                   c.course_name,
                   c.course_type,
                   c.credit,
                   o.planned_weeks,
                   o.effective_sessions,
                   o.pass_score,
                   tc.class_name,
                   t.term_name,
                   te.teacher_name,
                   s.student_no,
                   s.student_name
            from enrollment e
                     join teaching_class tc on tc.id = e.teaching_class_id
                     join course_offering o on o.id = e.offering_id
                     join course c on c.id = o.course_id
                     join term t on t.id = e.term_id
                     join teacher te on te.id = tc.teacher_id
                     join student s on s.id = e.student_id
            where e.id = ?
            """;

    public ReportFact build(Long studentId, Long enrollmentId) {
        ReportFact f = new ReportFact();
        List<Map<String, Object>> base = jdbc.queryForList(BASE_SQL, enrollmentId);
        if (base.isEmpty()) {
            throw new IllegalArgumentException("选课记录不存在：" + enrollmentId);
        }
        Map<String, Object> row = base.get(0);
        if (studentId != null && !studentId.equals(asLong(row.get("student_id")))) {
            throw new IllegalArgumentException("无权查看该选课记录的学情报告");
        }

        f.setEnrollmentId(asLong(row.get("enrollment_id")));
        f.setStudentId(asLong(row.get("student_id")));
        f.setStudentNo(asString(row.get("student_no")));
        f.setStudentName(asString(row.get("student_name")));
        f.setTeachingClassId(asLong(row.get("teaching_class_id")));
        f.setClassName(asString(row.get("class_name")));
        f.setOfferingId(asLong(row.get("offering_id")));
        f.setCourseId(asLong(row.get("course_id")));
        f.setCourseName(asString(row.get("course_name")));
        f.setCourseType(asString(row.get("course_type")));
        f.setCredit(asInt(row.get("credit")));
        f.setTermId(asLong(row.get("term_id")));
        f.setTermName(asString(row.get("term_name")));
        f.setTeacherName(asString(row.get("teacher_name")));
        f.setPlannedWeeks(asInt(row.get("planned_weeks")));
        f.setEffectiveSessions(asInt(row.get("effective_sessions")));
        f.setPassScore(asInt(row.get("pass_score")));

        fillScore(f);
        fillItems(f);
        fillGroupAndType(f);
        fillWeeks(f);
        fillRisk(f);
        fillAlert(f);
        fillProjects(f);
        return f;
    }

    // ---------------------------------------------------------------- 总分与班级对比
    private void fillScore(ReportFact f) {
        List<Map<String, Object>> rs = jdbc.queryForList("""
                select weighted_score, deduction_points, total_score,
                       absent_count, late_count, leave_count, allowed_absent,
                       hard_fail, is_fail, fail_reason
                from score_result
                where enrollment_id = ? and is_void = 0
                order by id desc limit 1
                """, f.getEnrollmentId());
        if (!rs.isEmpty()) {
            Map<String, Object> r = rs.get(0);
            f.setWeightedScore(asDecimal(r.get("weighted_score")));
            f.setDeductionPoints(asDecimal(r.get("deduction_points")));
            f.setTotalScore(asDecimal(r.get("total_score")));
            f.setAbsentCount(asInt(r.get("absent_count")));
            f.setLateCount(asInt(r.get("late_count")));
            f.setLeaveCount(asInt(r.get("leave_count")));
            f.setAllowedAbsent(asInt(r.get("allowed_absent")));
            f.setHardFail(asBool(r.get("hard_fail")));
            String reason = asString(r.get("fail_reason"));
            if (reason != null) {
                f.setFailReason(reason);
            }
        } else {
            // 没有定稿成绩时，用预警结论里的计数兜底
            List<Map<String, Object>> st = jdbc.queryForList("""
                    select absent_count, late_count, leave_count, allowed_absent, remaining_absent, risk_score
                    from risk_state where enrollment_id = ? and is_void = 0 order by id desc limit 1
                    """, f.getEnrollmentId());
            if (!st.isEmpty()) {
                f.setAbsentCount(asInt(st.get(0).get("absent_count")));
                f.setLateCount(asInt(st.get(0).get("late_count")));
                f.setLeaveCount(asInt(st.get(0).get("leave_count")));
                f.setAllowedAbsent(asInt(st.get(0).get("allowed_absent")));
                f.setRemainingAbsent(asInt(st.get(0).get("remaining_absent")));
            }
        }

        List<Map<String, Object>> agg = jdbc.queryForList("""
                select avg(sr.total_score) as avg_score, count(*) as size
                from score_result sr
                         join enrollment e2 on e2.id = sr.enrollment_id
                where e2.teaching_class_id = ? and sr.is_void = 0 and sr.total_score is not null
                """, f.getTeachingClassId());
        if (!agg.isEmpty()) {
            f.setClassAvg(asDecimal(agg.get(0).get("avg_score")));
            f.setClassSize(asInt(agg.get(0).get("size")));
        }
        if (f.getTotalScore() != null) {
            Integer better = jdbc.queryForObject("""
                    select count(*)
                    from score_result sr join enrollment e2 on e2.id = sr.enrollment_id
                    where e2.teaching_class_id = ? and sr.is_void = 0 and sr.total_score > ?
                    """, Integer.class, f.getTeachingClassId(), f.getTotalScore());
            f.setClassRank((better == null ? 0 : better) + 1);
        }
    }

    // ---------------------------------------------------------------- 成绩构成分项
    private void fillItems(ReportFact f) {
        List<StudentScoreVO> all;
        try {
            all = scoreMatrixService.studentScores(f.getStudentId());
        } catch (Exception e) {
            log.warn("[FactPack] 取成绩分项失败: {}", e.getMessage());
            return;
        }
        StudentScoreVO mine = all.stream()
                .filter(v -> f.getTeachingClassId().equals(v.getTeachingClassId()))
                .findFirst().orElse(null);
        if (mine == null || mine.getItems() == null) {
            return;
        }
        List<ReportFact.Item> items = new ArrayList<>();
        for (StudentScoreVO.Item it : mine.getItems()) {
            ReportFact.Item x = new ReportFact.Item();
            x.setName(it.getItemName());
            x.setPath(it.getPath());
            x.setScore(it.getScore());
            x.setWeight(it.getWeight());
            x.setMissing(it.getMissing());
            if (it.getScore() != null) {
                x.setRate(it.getScore().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            }
            items.add(x);
        }
        f.setItems(items);
    }

    // ---------------------------------------------------------------- 分组 / 题型
    private void fillGroupAndType(ReportFact f) {
        f.setGroups(mergeDim(
                "select q.group_name as name, coalesce(sum(sa.score),0) as got, coalesce(sum(q.full_score),0) as full, count(*) as qn " +
                        "from submission_answer sa join submission s on s.id = sa.submission_id join question q on q.id = sa.question_id " +
                        "where s.student_id = ? and s.teaching_class_id = ? group by q.group_name",
                "select q.group_name as name, coalesce(sum(sa.score),0) as got, coalesce(sum(q.full_score),0) as full, count(distinct s.student_id) as qn " +
                        "from submission_answer sa join submission s on s.id = sa.submission_id join question q on q.id = sa.question_id " +
                        "where s.teaching_class_id = ? group by q.group_name",
                f.getStudentId(), f.getTeachingClassId()));

        List<ReportFact.Dim> types = mergeDim(
                "select q.q_type as name, coalesce(sum(sa.score),0) as got, coalesce(sum(q.full_score),0) as full, count(*) as qn " +
                        "from submission_answer sa join submission s on s.id = sa.submission_id join question q on q.id = sa.question_id " +
                        "where s.student_id = ? and s.teaching_class_id = ? group by q.q_type",
                "select q.q_type as name, coalesce(sum(sa.score),0) as got, coalesce(sum(q.full_score),0) as full, count(distinct s.student_id) as qn " +
                        "from submission_answer sa join submission s on s.id = sa.submission_id join question q on q.id = sa.question_id " +
                        "where s.teaching_class_id = ? group by q.q_type",
                f.getStudentId(), f.getTeachingClassId());
        // 题型代码转中文，避免报告里出现 "SHORT 题型" 这种内部术语
        Map<String, String> typeLabels = Map.of("SINGLE", "单选题", "MULTI", "多选题",
                "JUDGE", "判断题", "BLANK", "填空题", "SHORT", "简答题");
        types.forEach(t -> t.setName(typeLabels.getOrDefault(t.getName(), t.getName())));
        f.setTypes(types);
    }

    private List<ReportFact.Dim> mergeDim(String mySql, String classSql, Long studentId, Long classId) {
        Map<String, ReportFact.Dim> map = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(mySql, studentId, classId)) {
            String name = asString(r.get("name"));
            if (name == null || name.isBlank()) {
                name = "未分组";
            }
            ReportFact.Dim d = new ReportFact.Dim();
            d.setName(name);
            d.setMyRate(ratio(r.get("got"), r.get("full")));
            d.setQuestionCount(asInt(r.get("qn")));
            map.put(name, d);
        }
        for (Map<String, Object> r : jdbc.queryForList(classSql, classId)) {
            String name = asString(r.get("name"));
            if (name == null || name.isBlank()) {
                name = "未分组";
            }
            ReportFact.Dim d = map.computeIfAbsent(name, k -> {
                ReportFact.Dim x = new ReportFact.Dim();
                x.setName(k);
                return x;
            });
            d.setClassRate(ratio(r.get("got"), r.get("full")));
        }
        for (ReportFact.Dim d : map.values()) {
            if (d.getMyRate() != null && d.getClassRate() != null) {
                d.setGap(d.getMyRate().subtract(d.getClassRate()));
            }
        }
        List<ReportFact.Dim> out = new ArrayList<>(map.values());
        out.sort(Comparator.comparing((ReportFact.Dim d) -> d.getMyRate() == null ? BigDecimal.ONE : d.getMyRate()));
        return out;
    }

    // ---------------------------------------------------------------- 周次趋势
    private void fillWeeks(ReportFact f) {
        Map<Integer, BigDecimal> mine = weekRates(
                "select l.week_no as w, coalesce(sum(sa.score),0) as got, coalesce(sum(q.full_score),0) as full " +
                        "from submission s join paper p on p.id = s.paper_id join submission_answer sa on sa.submission_id = s.id " +
                        "join question q on q.id = sa.question_id join course_lesson l on l.id = p.lesson_id " +
                        "where s.student_id = ? and s.teaching_class_id = ? group by l.week_no", f.getStudentId(), f.getTeachingClassId());
        Map<Integer, BigDecimal> klass = weekRates(
                "select l.week_no as w, coalesce(sum(sa.score),0) as got, coalesce(sum(q.full_score),0) as full " +
                        "from submission s join paper p on p.id = s.paper_id join submission_answer sa on sa.submission_id = s.id " +
                        "join question q on q.id = sa.question_id join course_lesson l on l.id = p.lesson_id " +
                        "where s.teaching_class_id = ? group by l.week_no", f.getTeachingClassId());
        List<ReportFact.Trend> list = new ArrayList<>();
        mine.keySet().stream().sorted().forEach(w -> {
            ReportFact.Trend t = new ReportFact.Trend();
            t.setWeek(w);
            t.setMyRate(mine.get(w));
            t.setClassRate(klass.get(w));
            list.add(t);
        });
        f.setWeeks(list);
    }

    private Map<Integer, BigDecimal> weekRates(String sql, Object... args) {
        Map<Integer, BigDecimal> map = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(sql, args)) {
            Integer w = asInt(r.get("w"));
            if (w != null) {
                map.put(w, ratio(r.get("got"), r.get("full")));
            }
        }
        return map;
    }

    // ---------------------------------------------------------------- 预警结论与事件
    private void fillRisk(ReportFact f) {
        List<Map<String, Object>> rs = jdbc.queryForList("""
                select risk_level, confidence_level, output_mode, final_exam_needed,
                       hard_fail, near_fail, remaining_absent, absent_count, late_count, leave_count, allowed_absent
                from risk_state where enrollment_id = ? and is_void = 0 order by id desc limit 1
                """, f.getEnrollmentId());
        if (rs.isEmpty()) {
            return;
        }
        Map<String, Object> r = rs.get(0);
        f.setRiskLevel(asString(r.get("risk_level")));
        f.setConfidenceLevel(asString(r.get("confidence_level")));
        f.setOutputMode(asString(r.get("output_mode")));
        f.setFinalExamNeeded(asDecimal(r.get("final_exam_needed")));
        f.setHardFail(asBool(r.get("hard_fail")));
        f.setNearFail(asBool(r.get("near_fail")));
        if (f.getRemainingAbsent() == null) {
            f.setRemainingAbsent(asInt(r.get("remaining_absent")));
        }
        if (f.getAbsentCount() == null) {
            f.setAbsentCount(asInt(r.get("absent_count")));
            f.setLateCount(asInt(r.get("late_count")));
            f.setLeaveCount(asInt(r.get("leave_count")));
            f.setAllowedAbsent(asInt(r.get("allowed_absent")));
        }
    }

    private void fillAlert(ReportFact f) {
        List<Map<String, Object>> rs = jdbc.queryForList("""
                select alert_level, alert_reason, status, teacher_reply, student_response
                from alert_record
                where enrollment_id = ? and is_void = 0
                order by is_current desc, id desc limit 1
                """, f.getEnrollmentId());
        if (rs.isEmpty()) {
            return;
        }
        Map<String, Object> r = rs.get(0);
        f.setAlertLevel(asString(r.get("alert_level")));
        f.setAlertReason(asString(r.get("alert_reason")));
        f.setAlertStatus(asString(r.get("status")));
        f.setTeacherReply(asString(r.get("teacher_reply")));
        String resp = asString(r.get("student_response"));
        f.setAppealPending(resp != null && !resp.isBlank()
                && (f.getTeacherReply() == null || f.getTeacherReply().isBlank()));
    }

    // ---------------------------------------------------------------- 项目
    private void fillProjects(ReportFact f) {
        List<Map<String, Object>> rs = jdbc.queryForList("""
                select pt.project_name, ps.project_score, ps.submit_status, ps.team_id, pc.contribution_json
                from project_submission ps
                         join project_task pt on pt.id = ps.project_id
                         left join project_contribution pc on pc.team_id = ps.team_id
                where ps.student_id = ? and pt.offering_id = ?
                order by pt.id
                """, f.getStudentId(), f.getOfferingId());
        List<ReportFact.Project> list = new ArrayList<>();
        for (Map<String, Object> r : rs) {
            ReportFact.Project p = new ReportFact.Project();
            p.setProjectName(asString(r.get("project_name")));
            p.setTeamScore(asDecimal(r.get("project_score")));
            p.setSubmitStatus(asString(r.get("submit_status")));
            p.setRatio(ratioOf(asString(r.get("contribution_json")), f.getStudentId()));
            list.add(p);
        }
        f.setProjects(list);
    }

    /** 从贡献度 JSON 里取本人在该队的比例 */
    private BigDecimal ratioOf(String json, Long studentId) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root.isArray()) {
                for (JsonNode n : root) {
                    if (studentId != null && n.path("studentId").asLong(0) == studentId) {
                        String r = n.path("ratio").asText("");
                        if (!r.isBlank()) {
                            return new BigDecimal(r).setScale(2, RoundingMode.HALF_UP);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[FactPack] 贡献度解析失败: {}", e.getMessage());
        }
        return null;
    }

    // ---------------------------------------------------------------- 类型转换
    private static BigDecimal ratio(Object got, Object full) {
        BigDecimal g = asDecimal(got);
        BigDecimal f = asDecimal(full);
        if (g == null || f == null || f.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return g.divide(f, 4, RoundingMode.HALF_UP);
    }

    static BigDecimal asDecimal(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof BigDecimal b) {
            return b;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(String.valueOf(v)).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    static Integer asInt(Object v) {
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

    static Long asLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }

    static Boolean asBool(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Number n) {
            return n.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(v));
    }

    static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
