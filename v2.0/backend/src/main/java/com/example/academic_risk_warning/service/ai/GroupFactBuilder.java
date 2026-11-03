package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.vo.ai.AdminClassFact;
import com.example.academic_risk_warning.vo.ai.ClassFact;
import com.example.academic_risk_warning.vo.ai.OfferingFact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 群体事实包构建：教学班（一门课×一个班）与行政班（一个班×多门课）。
 *
 * <p>只用已有表。薄弱项用 <b>课程分组（question.group_name）+ 题型</b> 表达，
 * 不依赖知识点本体。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupFactBuilder {

    private final JdbcTemplate jdbc;

    private static final Map<String, String> TYPE_LABELS = Map.of(
            "SINGLE", "单选题", "MULTI", "多选题", "JUDGE", "判断题",
            "BLANK", "填空题", "SHORT", "简答题");

    // ============================================================ 教学班
    public ClassFact buildClass(Long teachingClassId) {
        List<Map<String, Object>> base = jdbc.queryForList("""
                select tc.id as teaching_class_id, tc.class_code, tc.class_name, tc.teacher_id,
                       o.id as offering_id, o.term_id, o.planned_weeks, o.pass_score,
                       c.id as course_id, c.course_name, c.course_type, c.credit,
                       t.term_name, te.teacher_name
                from teaching_class tc
                         join course_offering o on o.id = tc.offering_id
                         join course c on c.id = o.course_id
                         join term t on t.id = o.term_id
                         join teacher te on te.id = tc.teacher_id
                where tc.id = ?
                """, teachingClassId);
        if (base.isEmpty()) {
            throw new IllegalArgumentException("教学班不存在：" + teachingClassId);
        }
        Map<String, Object> r = base.get(0);
        ClassFact f = new ClassFact();
        f.setTeachingClassId(asLong(r.get("teaching_class_id")));
        f.setClassCode(asString(r.get("class_code")));
        f.setClassName(asString(r.get("class_name")));
        f.setOfferingId(asLong(r.get("offering_id")));
        f.setCourseId(asLong(r.get("course_id")));
        f.setCourseName(asString(r.get("course_name")));
        f.setCourseType(asString(r.get("course_type")));
        f.setCredit(asInt(r.get("credit")));
        f.setTermId(asLong(r.get("term_id")));
        f.setTermName(asString(r.get("term_name")));
        f.setTeacherId(asLong(r.get("teacher_id")));
        f.setTeacherName(asString(r.get("teacher_name")));
        f.setPlannedWeeks(asInt(r.get("planned_weeks")));
        f.setPassScore(asInt(r.get("pass_score")));

        // 人数
        f.setStudentCount(asInt(jdbc.queryForObject(
                "select count(*) from enrollment where teaching_class_id = ? and status in ('ACTIVE','ENROLLED')",
                Integer.class, teachingClassId)));

        // 风险分布（每名学生一条 risk_state，按最新）
        Map<String, Object> dist = new HashMap<>();
        for (Map<String, Object> row : jdbc.queryForList("""
                select rs.risk_level, count(*) as n
                from risk_state rs
                         join enrollment e on e.id = rs.enrollment_id
                where e.teaching_class_id = ? and rs.is_void = 0
                group by rs.risk_level
                """, teachingClassId)) {
            dist.put(asString(row.get("risk_level")), asInt(row.get("n")));
        }
        int red = nz(asInt(dist.get("RED")));
        int yellow = nz(asInt(dist.get("YELLOW")));
        int green = nz(asInt(dist.get("GREEN")));
        f.setRedCount(red);
        f.setYellowCount(yellow);
        f.setGreenCount(green);
        f.setUnknownCount(Math.max(0, nz(f.getStudentCount()) - red - yellow - green));

        // 总分与考勤
        List<Map<String, Object>> agg = jdbc.queryForList("""
                select avg(sr.total_score) as avg_score, min(sr.total_score) as min_score, max(sr.total_score) as max_score,
                       sum(case when sr.hard_fail = 1 then 1 else 0 end) as hard_fail,
                       sum(case when sr.absent_count > sr.allowed_absent then 1 else 0 end) as over_limit,
                       avg(sr.absent_count) as avg_absent, max(sr.absent_count) as max_absent,
                       sum(sr.late_count) as late_total
                from score_result sr join enrollment e on e.id = sr.enrollment_id
                where e.teaching_class_id = ? and sr.is_void = 0
                """, teachingClassId);
        if (!agg.isEmpty() && agg.get(0).get("avg_score") != null) {
            Map<String, Object> a = agg.get(0);
            f.setAvgScore(dec(a.get("avg_score")));
            f.setMinScore(dec(a.get("min_score")));
            f.setMaxScore(dec(a.get("max_score")));
            f.setHardFailCount(asInt(a.get("hard_fail")));
            f.setAbsentOverLimitCount(asInt(a.get("over_limit")));
            f.setAvgAbsent(dec(a.get("avg_absent")));
            f.setMaxAbsent(asInt(a.get("max_absent")));
            f.setLateTotal(asInt(a.get("late_total")));
        }

        // 共性难点：分组 / 题型（全班聚合）
        f.setWeakGroups(dims("""
                select coalesce(q.group_name,'未分组') as name, coalesce(sum(sa.score),0) as got,
                       coalesce(sum(q.full_score),0) as full, count(*) as qn
                from submission_answer sa
                         join submission s on s.id = sa.submission_id
                         join question q on q.id = sa.question_id
                where s.teaching_class_id = ?
                group by coalesce(q.group_name,'未分组')
                having sum(q.full_score) > 0
                order by got / full asc
                limit 5
                """, teachingClassId));
        f.setWeakTypes(dims("""
                select q.q_type as name, coalesce(sum(sa.score),0) as got,
                       coalesce(sum(q.full_score),0) as full, count(*) as qn
                from submission_answer sa
                         join submission s on s.id = sa.submission_id
                         join question q on q.id = sa.question_id
                where s.teaching_class_id = ?
                group by q.q_type
                having sum(q.full_score) > 0
                order by got / full asc
                """, teachingClassId));

        // 手工登分项的低分项
        List<ClassFact.Item> items = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList("""
                select ssi.item_name, avg(si.score) as avg_score, count(*) as n
                from score_item si
                         join score_scheme_item ssi on ssi.id = si.scheme_item_id
                         join enrollment e on e.id = si.enrollment_id
                where e.teaching_class_id = ? and si.is_void = 0 and si.score is not null
                group by ssi.id, ssi.item_name
                having count(*) >= 3
                order by avg(si.score) asc
                limit 3
                """, teachingClassId)) {
            ClassFact.Item it = new ClassFact.Item();
            it.setName(asString(row.get("item_name")));
            it.setAvgScore(dec(row.get("avg_score")));
            it.setSampleCount(asInt(row.get("n")));
            items.add(it);
        }
        f.setWeakItems(items);

        // 未登分项：应交数量 - 已交数量
        Integer expected = jdbc.queryForObject("""
                select coalesce(sum(ssi.expected_times), 0)
                from score_scheme_item ssi
                         join score_scheme ss on ss.id = ssi.scheme_id
                         join course_offering o on o.group_id = ss.group_id
                where o.id = ? and ssi.data_source = 'MANUAL' and ssi.status = 'ACTIVE'
                """, Integer.class, f.getOfferingId());
        Integer graded = jdbc.queryForObject("""
                select count(*) from score_item si join enrollment e on e.id = si.enrollment_id
                where e.teaching_class_id = ? and si.is_void = 0
                """, Integer.class, teachingClassId);
        int exp = expected == null ? 0 : expected;
        int got = graded == null ? 0 : graded;
        f.setUngradedItemCount(exp > 0 && got < exp ? 1 : 0);

        // 预警事件
        f.setActiveAlertCount(asInt(jdbc.queryForObject(
                "select count(*) from alert_record where teaching_class_id = ? and status in ('ACTIVE','HANDLING') and is_void = 0",
                Integer.class, teachingClassId)));

        // 重点关注名单
        List<ClassFact.Student> watch = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList("""
                select s.id as student_id, s.student_no, s.student_name, rs.risk_level, rs.risk_score,
                       sr.total_score, rs.absent_count, rs.allowed_absent, rs.final_exam_needed, rs.near_fail
                from risk_state rs
                         join enrollment e on e.id = rs.enrollment_id
                         join student s on s.id = rs.student_id
                         left join score_result sr on sr.enrollment_id = e.id and sr.is_void = 0
                where e.teaching_class_id = ? and rs.is_void = 0
                order by case rs.risk_level when 'RED' then 1 when 'YELLOW' then 2 else 3 end,
                         sr.total_score asc
                limit 10
                """, teachingClassId)) {
            ClassFact.Student st = new ClassFact.Student();
            st.setStudentId(asLong(row.get("student_id")));
            st.setStudentNo(asString(row.get("student_no")));
            st.setStudentName(asString(row.get("student_name")));
            st.setRiskLevel(asString(row.get("risk_level")));
            st.setTotalScore(dec(row.get("total_score")));
            st.setAbsentCount(asInt(row.get("absent_count")));
            st.setAllowedAbsent(asInt(row.get("allowed_absent")));
            Integer ab = st.getAbsentCount();
            Integer allow = st.getAllowedAbsent();
            if (ab != null && allow != null && ab > allow) {
                st.setReason("缺勤 " + ab + " 次已超上限 " + allow + " 次");
            } else if (Boolean.TRUE.equals(bool(row.get("near_fail")))) {
                st.setReason("总分接近及格线");
            } else if (st.getTotalScore() != null) {
                st.setReason("总分偏低");
            }
            watch.add(st);
        }
        f.setWatchList(watch);
        return f;
    }

    private List<ClassFact.Dim> dims(String sql, Long classId) {
        List<ClassFact.Dim> out = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList(sql, classId)) {
            ClassFact.Dim d = new ClassFact.Dim();
            String name = asString(row.get("name"));
            if (name == null || name.isBlank()) {
                name = "未分组";
            }
            d.setName(TYPE_LABELS.getOrDefault(name, name));
            d.setRate(rate(row.get("got"), row.get("full")));
            d.setQuestionCount(asInt(row.get("qn")));
            out.add(d);
        }
        return out;
    }

    // ============================================================ 行政班
    public AdminClassFact buildAdminClass(Long adminClassId) {
        List<Map<String, Object>> base = jdbc.queryForList("""
                select ac.id, ac.class_code, ac.class_name, ac.grade_year, m.major_name,
                       t.id as term_id, t.term_name
                from administrative_class ac
                         left join major m on m.id = ac.major_id
                         left join term t on t.is_current = 1
                where ac.id = ?
                """, adminClassId);
        if (base.isEmpty()) {
            throw new IllegalArgumentException("行政班不存在：" + adminClassId);
        }
        Map<String, Object> r = base.get(0);
        AdminClassFact f = new AdminClassFact();
        f.setAdminClassId(asLong(r.get("id")));
        f.setClassCode(asString(r.get("class_code")));
        f.setClassName(asString(r.get("class_name")));
        f.setGradeYear(asInt(r.get("grade_year")));
        f.setMajorName(asString(r.get("major_name")));
        f.setTermId(asLong(r.get("term_id")));
        f.setTermName(asString(r.get("term_name")));

        // 辅导员
        List<Map<String, Object>> cs = jdbc.queryForList("""
                select c.id, c.counselor_name from counselor_class cc join counselor c on c.id = cc.counselor_id
                where cc.admin_class_id = ? order by cc.is_primary desc limit 1
                """, adminClassId);
        if (!cs.isEmpty()) {
            f.setCounselorId(asLong(cs.get(0).get("id")));
            f.setCounselorName(asString(cs.get(0).get("counselor_name")));
        }

        // 学生数（本班、在读；学籍状态用 IN_SCHOOL，退学/毕业的不算）
        f.setStudentCount(asInt(jdbc.queryForObject(
                "select count(*) from student where admin_class_id = ? and status = 'IN_SCHOOL'",
                Integer.class, adminClassId)));

        // 选课与风险分布：只统计当前学期
        List<Map<String, Object>> agg = jdbc.queryForList("""
                select count(*) as total,
                       sum(case when rs.risk_level = 'RED' then 1 else 0 end) as red,
                       sum(case when rs.risk_level = 'YELLOW' then 1 else 0 end) as yellow,
                       sum(case when rs.risk_level = 'GREEN' then 1 else 0 end) as green,
                       sum(case when rs.risk_level is null then 1 else 0 end) as unknown,
                       sum(case when sr.hard_fail = 1 then 1 else 0 end) as hard_fail,
                       avg(sr.total_score) as avg_score,
                       count(distinct e.offering_id) as courses
                from enrollment e
                         join student s on s.id = e.student_id
                         left join risk_state rs on rs.enrollment_id = e.id and rs.is_void = 0
                         left join score_result sr on sr.enrollment_id = e.id and sr.is_void = 0
                         join term t on t.id = e.term_id and t.is_current = 1
                where s.admin_class_id = ?
                """, adminClassId);
        if (!agg.isEmpty() && agg.get(0).get("total") != null) {
            Map<String, Object> a = agg.get(0);
            int total = nz(asInt(a.get("total")));
            f.setEnrollmentCount(total);
            f.setRedCount(nz(asInt(a.get("red"))));
            f.setYellowCount(nz(asInt(a.get("yellow"))));
            f.setGreenCount(nz(asInt(a.get("green"))));
            f.setUnknownCount(nz(asInt(a.get("unknown"))));
            f.setHardFailCount(nz(asInt(a.get("hard_fail"))));
            f.setAvgScore(dec(a.get("avg_score")));
            f.setCourseCount(nz(asInt(a.get("courses"))));
            if (total > 0) {
                f.setFailRate(BigDecimal.valueOf(nz(asInt(a.get("red"))))
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP));
            }
        }

        // 集中风险课程
        List<AdminClassFact.CourseRisk> risks = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList("""
                select c.id as course_id, c.course_name,
                       sum(case when rs.risk_level = 'RED' then 1 else 0 end) as red,
                       sum(case when rs.risk_level = 'YELLOW' then 1 else 0 end) as yellow,
                       count(*) as total
                from enrollment e
                         join student s on s.id = e.student_id
                         join course_offering o on o.id = e.offering_id
                         join course c on c.id = o.course_id
                         join term t on t.id = e.term_id and t.is_current = 1
                         left join risk_state rs on rs.enrollment_id = e.id and rs.is_void = 0
                where s.admin_class_id = ?
                group by c.id, c.course_name
                order by sum(case when rs.risk_level in ('RED','YELLOW') then 1 else 0 end) desc
                limit 3
                """, adminClassId)) {
            AdminClassFact.CourseRisk cr = new AdminClassFact.CourseRisk();
            cr.setCourseId(asLong(row.get("course_id")));
            cr.setCourseName(asString(row.get("course_name")));
            cr.setRed(nz(asInt(row.get("red"))));
            cr.setYellow(nz(asInt(row.get("yellow"))));
            cr.setTotal(nz(asInt(row.get("total"))));
            if (cr.getTotal() != null && cr.getTotal() > 0) {
                cr.setFailRate(BigDecimal.valueOf(cr.getRed())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(cr.getTotal()), 1, RoundingMode.HALF_UP));
            }
            risks.add(cr);
        }
        f.setCourseRisks(risks);

        // 多门预警学生（≥2 门红/黄）
        List<AdminClassFact.Student> multi = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList("""
                select s.id as student_id, s.student_no, s.student_name,
                       count(*) as risk_count, min(sr.total_score) as min_score,
                       group_concat(c.course_name order by c.course_name separator '、') as courses
                from enrollment e
                         join student s on s.id = e.student_id
                         join course_offering o on o.id = e.offering_id
                         join course c on c.id = o.course_id
                         join term t on t.id = e.term_id and t.is_current = 1
                         join risk_state rs on rs.enrollment_id = e.id and rs.is_void = 0
                         left join score_result sr on sr.enrollment_id = e.id and sr.is_void = 0
                where s.admin_class_id = ? and rs.risk_level in ('RED','YELLOW')
                group by s.id, s.student_no, s.student_name
                having count(*) >= 2
                order by count(*) desc, min(sr.total_score) asc
                limit 10
                """, adminClassId)) {
            AdminClassFact.Student st = new AdminClassFact.Student();
            st.setStudentId(asLong(row.get("student_id")));
            st.setStudentNo(asString(row.get("student_no")));
            st.setStudentName(asString(row.get("student_name")));
            st.setRiskCount(asInt(row.get("risk_count")));
            st.setMinScore(dec(row.get("min_score")));
            String courses = asString(row.get("courses"));
            st.setRiskCourses(courses != null && courses.length() > 120 ? courses.substring(0, 119) + "…" : courses);
            multi.add(st);
        }
        f.setMultiRiskStudents(multi);

        // 未关闭的预警事件数
        f.setActiveAlertCount(asInt(jdbc.queryForObject("""
                select count(*) from alert_record ar join student s on s.id = ar.student_id
                where s.admin_class_id = ? and ar.status in ('ACTIVE','HANDLING') and ar.is_void = 0
                """, Integer.class, adminClassId)));

        return f;
    }

    // ============================================================ 课程（一门课 × 多个班）
    /**
     * 课程维度事实包：把同一门课下、该教师名下的各教学班事实包聚合起来。
     *
     * <p>口径说明：人数、红黄绿、硬性挂科按<b>选课记录</b>直接相加；
     * 平均分按人数加权，最低/最高分取极值；共性难点按题量加权。
     * 不重新查一遍底表，避免与教学班报告的数字对不上。</p>
     *
     * @param teacherId 传 null 表示不限教师（管理端视角）
     */
    public OfferingFact buildOffering(Long offeringId, Long teacherId) {
        List<Map<String, Object>> base = jdbc.queryForList("""
                select o.id as offering_id, o.term_id, o.planned_weeks, o.pass_score,
                       c.id as course_id, c.course_name, c.course_type, c.credit,
                       t.term_name
                from course_offering o
                         join course c on c.id = o.course_id
                         join term t on t.id = o.term_id
                where o.id = ?
                """, offeringId);
        if (base.isEmpty()) {
            throw new IllegalArgumentException("开课不存在：" + offeringId);
        }
        Map<String, Object> r = base.get(0);
        OfferingFact f = new OfferingFact();
        f.setOfferingId(asLong(r.get("offering_id")));
        f.setCourseId(asLong(r.get("course_id")));
        f.setCourseName(asString(r.get("course_name")));
        f.setCourseType(asString(r.get("course_type")));
        f.setCredit(asInt(r.get("credit")));
        f.setTermId(asLong(r.get("term_id")));
        f.setTermName(asString(r.get("term_name")));
        f.setPlannedWeeks(asInt(r.get("planned_weeks")));
        f.setPassScore(asInt(r.get("pass_score")));
        f.setTeacherId(teacherId);
        if (teacherId != null) {
            List<Map<String, Object>> ts = jdbc.queryForList(
                    "select teacher_name from teacher where id = ?", teacherId);
            if (!ts.isEmpty()) {
                f.setTeacherName(asString(ts.get(0).get("teacher_name")));
            }
        }

        List<Long> tcIds = jdbc.queryForList("""
                select id from teaching_class
                where offering_id = ? and (? is null or teacher_id = ?)
                order by class_code
                """, Long.class, offeringId, teacherId, teacherId);
        if (tcIds.isEmpty()) {
            throw new IllegalArgumentException("这门课下没有你的教学班");
        }

        List<ClassFact> facts = new ArrayList<>();
        for (Long tcId : tcIds) {
            facts.add(buildClass(tcId));
        }
        f.setClassCount(facts.size());

        int students = 0;
        int red = 0, yellow = 0, green = 0, unknown = 0, hardFail = 0, overLimit = 0, alerts = 0, ungraded = 0;
        BigDecimal scoreSum = BigDecimal.ZERO;
        BigDecimal minScore = null;
        BigDecimal maxScore = null;
        for (ClassFact cf : facts) {
            int n = nz(cf.getStudentCount());
            students += n;
            red += nz(cf.getRedCount());
            yellow += nz(cf.getYellowCount());
            green += nz(cf.getGreenCount());
            unknown += nz(cf.getUnknownCount());
            hardFail += nz(cf.getHardFailCount());
            overLimit += nz(cf.getAbsentOverLimitCount());
            alerts += nz(cf.getActiveAlertCount());
            if (nz(cf.getUngradedItemCount()) > 0) {
                ungraded++;
            }
            if (cf.getAvgScore() != null && n > 0) {
                scoreSum = scoreSum.add(cf.getAvgScore().multiply(BigDecimal.valueOf(n)));
            }
            if (cf.getMinScore() != null && (minScore == null || cf.getMinScore().compareTo(minScore) < 0)) {
                minScore = cf.getMinScore();
            }
            if (cf.getMaxScore() != null && (maxScore == null || cf.getMaxScore().compareTo(maxScore) > 0)) {
                maxScore = cf.getMaxScore();
            }
            OfferingFact.ClassRow row = new OfferingFact.ClassRow();
            row.setTeachingClassId(cf.getTeachingClassId());
            row.setClassCode(cf.getClassCode());
            row.setClassName(cf.getClassName());
            row.setStudentCount(cf.getStudentCount());
            row.setRed(cf.getRedCount());
            row.setYellow(cf.getYellowCount());
            row.setGreen(cf.getGreenCount());
            row.setHardFail(cf.getHardFailCount());
            row.setAvgScore(cf.getAvgScore());
            row.setMinScore(cf.getMinScore());
            row.setMaxScore(cf.getMaxScore());
            row.setAbsentOverLimitCount(cf.getAbsentOverLimitCount());
            row.setActiveAlertCount(cf.getActiveAlertCount());
            f.getClasses().add(row);
        }
        f.setStudentCount(students);
        f.setRedCount(red);
        f.setYellowCount(yellow);
        f.setGreenCount(green);
        f.setUnknownCount(unknown);
        f.setHardFailCount(hardFail);
        f.setAbsentOverLimitCount(overLimit);
        f.setActiveAlertCount(alerts);
        f.setUngradedItemCount(ungraded);
        f.setMinScore(minScore);
        f.setMaxScore(maxScore);
        if (students > 0) {
            f.setAvgScore(scoreSum.divide(BigDecimal.valueOf(students), 2, RoundingMode.HALF_UP));
            f.setFailRate(BigDecimal.valueOf(red).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(students), 1, RoundingMode.HALF_UP));
        }
        // 红黄灯多的班排前面，方便"哪个班最吃力"一眼看出来
        f.getClasses().sort(Comparator
                .comparingInt((OfferingFact.ClassRow x) -> nz(x.getRed()) * 2 + nz(x.getYellow())).reversed()
                .thenComparing(x -> x.getClassName() == null ? "" : x.getClassName()));

        // 共性难点：同名分组/题型按题量加权
        f.setWeakGroups(mergeDims(facts, true));
        f.setWeakTypes(mergeDims(facts, false));

        // 重点关注：跨班取红灯优先
        List<OfferingFact.Student> watch = new ArrayList<>();
        for (ClassFact cf : facts) {
            for (ClassFact.Student s : cf.getWatchList()) {
                if (!"RED".equals(s.getRiskLevel()) && !"YELLOW".equals(s.getRiskLevel())) {
                    continue;
                }
                OfferingFact.Student o = new OfferingFact.Student();
                o.setStudentId(s.getStudentId());
                o.setTeachingClassId(cf.getTeachingClassId());
                o.setClassName(cf.getClassName());
                o.setStudentName(s.getStudentName());
                o.setRiskLevel(s.getRiskLevel());
                o.setTotalScore(s.getTotalScore());
                o.setReason(s.getReason());
                watch.add(o);
            }
        }
        watch.sort(Comparator
                .comparingInt((OfferingFact.Student s) -> "RED".equals(s.getRiskLevel()) ? 0 : 1)
                .thenComparing(s -> s.getTotalScore() == null ? BigDecimal.valueOf(999) : s.getTotalScore()));
        f.setWatchList(watch.size() > 10 ? new ArrayList<>(watch.subList(0, 10)) : watch);
        return f;
    }

    /** 各教学班的分组/题型按题量加权合并（同名相加） */
    private List<OfferingFact.Dim> mergeDims(List<ClassFact> facts, boolean group) {
        Map<String, BigDecimal[]> acc = new LinkedHashMap<>();
        for (ClassFact cf : facts) {
            for (ClassFact.Dim d : (group ? cf.getWeakGroups() : cf.getWeakTypes())) {
                if (d.getName() == null || d.getRate() == null) {
                    continue;
                }
                int qn = Math.max(1, nz(d.getQuestionCount()));
                BigDecimal[] cur = acc.computeIfAbsent(d.getName(),
                        k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                cur[0] = cur[0].add(d.getRate().multiply(BigDecimal.valueOf(qn)));
                cur[1] = cur[1].add(BigDecimal.valueOf(qn));
            }
        }
        List<OfferingFact.Dim> out = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : acc.entrySet()) {
            BigDecimal weight = e.getValue()[1];
            if (weight.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            OfferingFact.Dim d = new OfferingFact.Dim();
            d.setName(e.getKey());
            d.setRate(e.getValue()[0].divide(weight, 4, RoundingMode.HALF_UP));
            d.setQuestionCount(weight.intValue());
            out.add(d);
        }
        out.sort(Comparator.comparing(OfferingFact.Dim::getRate));
        return out.size() > 5 && group ? new ArrayList<>(out.subList(0, 5)) : out;
    }

    // ============================================================ 工具
    private static BigDecimal rate(Object got, Object full) {
        BigDecimal g = dec(got);
        BigDecimal f2 = dec(full);
        if (g == null || f2 == null || f2.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return g.divide(f2, 4, RoundingMode.HALF_UP);
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static BigDecimal dec(Object v) {
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

    private static Integer asInt(Object v) {
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

    private static Long asLong(Object v) {
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

    private static Boolean bool(Object v) {
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

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
