package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.SyntheticGenerateRequest;
import com.example.academic_risk_warning.entity.AttendanceRecord;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseGroup;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.CourseSession;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.Paper;
import com.example.academic_risk_warning.entity.ProjectTask;
import com.example.academic_risk_warning.entity.Question;
import com.example.academic_risk_warning.entity.ScoreItem;
import com.example.academic_risk_warning.entity.ScoreResult;
import com.example.academic_risk_warning.entity.ScoreScheme;
import com.example.academic_risk_warning.entity.ScoreSchemeItem;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.Submission;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeacherStyle;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.entity.Term;
import com.example.academic_risk_warning.mapper.AttendanceRecordMapper;
import com.example.academic_risk_warning.mapper.CourseGroupMapper;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.CourseSessionMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.PaperMapper;
import com.example.academic_risk_warning.mapper.QuestionMapper;
import com.example.academic_risk_warning.mapper.ScoreItemMapper;
import com.example.academic_risk_warning.mapper.ScoreResultMapper;
import com.example.academic_risk_warning.mapper.ScoreSchemeItemMapper;
import com.example.academic_risk_warning.mapper.ScoreSchemeMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.SubmissionMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.mapper.TeacherStyleMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.mapper.TermMapper;
import com.example.academic_risk_warning.vo.SyntheticResultVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 合成数据生成器（第一阶段 1-17）。
 * 目标：一笔生成「720 学生 × 6 门课 × 每生 6 门 × 32 课次」的联调数据，并**注入教师尺度真值**，
 * 供第二阶段验证 Δ 估计是否收敛。
 *
 * 做法：每个教师有一个已知给分偏移（严格 -8 / 宽松 +8 之间），生成分数时叠加该偏移，
 *       同时把真值写进 teacher_style（engine_version = SYNTH-TRUTH），后续用它对比估计值。
 *
 * 安全：只操作 code 以 SYN- 开头的独立学期；reset=true 时只清该学期的生成数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyntheticDataService {

    private static final String[] COURSE_NAMES = {
            "高等数学（一）", "大学物理（二）", "程序设计基础", "数据结构", "计算机网络", "数据库原理"
    };
    private static final String[] COURSE_TYPES = {"EXAM", "EXAM", "ASSESS", "EXAM", "EXAM", "ASSESS"};
    private static final BigDecimal[] TEACHER_BIAS = {
            new BigDecimal("-8"), new BigDecimal("6"), new BigDecimal("0"),
            new BigDecimal("3"), new BigDecimal("-5"), new BigDecimal("4"),
            new BigDecimal("0"), new BigDecimal("-3"), new BigDecimal("8")
    };

    private final TermMapper termMapper;
    private final CourseMapper courseMapper;
    private final CourseGroupMapper groupMapper;
    private final CourseOfferingMapper offeringMapper;
    private final TeachingClassMapper classMapper;
    private final CourseSessionMapper sessionMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final ScoreSchemeMapper schemeMapper;
    private final ScoreSchemeItemMapper schemeItemMapper;
    private final ScoreItemMapper scoreItemMapper;
    private final PaperMapper paperMapper;
    private final QuestionMapper questionMapper;
    private final SubmissionMapper submissionMapper;
    private final AttendanceRecordMapper attendanceMapper;
    private final ScoreResultMapper scoreResultMapper;
    private final TeacherStyleMapper teacherStyleMapper;
    private final ScoreMatrixService scoreMatrixService;

    // ============================================================ 入口
    @Transactional
    public SyntheticResultVO generate(SyntheticGenerateRequest req) {
        long start = System.currentTimeMillis();
        int studentCount = req == null || req.getStudentCount() == null ? 720 : req.getStudentCount();
        int courseCount = req == null || req.getCourseCount() == null ? 6 : req.getCourseCount();
        int sessions = req == null || req.getSessionsPerCourse() == null ? 32 : req.getSessionsPerCourse();
        int classesPerCourse = req == null || req.getClassesPerCourse() == null ? 3 : req.getClassesPerCourse();
        int attendanceSessions = req == null || req.getAttendanceSessions() == null ? 10 : req.getAttendanceSessions();
        boolean withScores = req == null || req.getGenerateScores() == null || req.getGenerateScores();
        boolean reset = req != null && Boolean.TRUE.equals(req.getReset());
        String termCode = req == null || req.getTermCode() == null || req.getTermCode().isBlank()
                ? "SYN-2027-1" : req.getTermCode().trim();
        String termName = req == null || req.getTermName() == null || req.getTermName().isBlank()
                ? "2027-2028学年-第1学期（合成数据）" : req.getTermName();

        if (studentCount <= 0 || studentCount > 2000) {
            throw new BizException("学生数需在 1~2000 之间");
        }
        if (courseCount <= 0 || courseCount > COURSE_NAMES.length) {
            throw new BizException("课程数需在 1~" + COURSE_NAMES.length + " 之间");
        }
        if (classesPerCourse <= 0 || classesPerCourse > 10) {
            throw new BizException("每门课的教学班数需在 1~10 之间");
        }
        List<String> notes = new ArrayList<>();

        Term term = termMapper.selectOne(Wrappers.<Term>lambdaQuery()
                .eq(Term::getTermCode, termCode).last("limit 1"));
        if (term == null) {
            term = new Term();
            term.setTermCode(termCode);
            term.setTermName(termName);
            term.setSchoolYear("2027-2028");
            term.setTermNo(1);
            term.setStartDate(LocalDate.of(2027, 9, 6));
            term.setEndDate(LocalDate.of(2027, 12, 26));
            term.setTotalWeeks(16);
            term.setIsCurrent(0);
            term.setStatus("ACTIVE");
            termMapper.insert(term);
        } else if (reset) {
            clearTerm(term.getId());
            notes.add("已清理该学期此前生成的数据");
        } else if (offeringMapper.selectCount(Wrappers.<CourseOffering>lambdaQuery()
                .eq(CourseOffering::getTermId, term.getId())).intValue() > 0) {
            throw new BizException("该学期已有生成数据；如需重新生成请带 reset=true");
        }

        List<Student> students = studentMapper.selectList(Wrappers.<Student>lambdaQuery()
                .orderByAsc(Student::getId).last("limit " + studentCount));
        if (students.size() < studentCount) {
            throw new BizException("学生不足：" + students.size() + " < " + studentCount);
        }
        List<Teacher> teachers = teacherMapper.selectList(Wrappers.<Teacher>lambdaQuery()
                .orderByAsc(Teacher::getId));
        if (teachers.isEmpty()) {
            throw new BizException("没有教师档案，无法生成");
        }

        SyntheticResultVO out = new SyntheticResultVO();
        out.setTermId(term.getId());
        out.setTermCode(termCode);
        out.setTermName(term.getTermName());
        out.setStudents(students.size());
        int enrollments = 0;
        int sessionCount = 0;
        int attendance = 0;
        int papers = 0;
        int submissions = 0;
        int scoreItems = 0;
        int scoreResults = 0;
        int styles = 0;
        int classTotal = 0;

        for (int ci = 0; ci < courseCount; ci++) {
            String courseName = COURSE_NAMES[ci];
            Course course = courseMapper.selectOne(Wrappers.<Course>lambdaQuery()
                    .eq(Course::getCourseName, courseName).last("limit 1"));
            if (course == null) {
                course = new Course();
                course.setCourseName(courseName);
                course.setCourseType(COURSE_TYPES[ci]);
                course.setCredit(new BigDecimal("4.0"));
                course.setCollegeName("计算机学院");
                course.setStatus("ACTIVE");
                courseMapper.insert(course);
            }
            CourseGroup group = groupMapper.selectOne(Wrappers.<CourseGroup>lambdaQuery()
                    .eq(CourseGroup::getCourseId, course.getId())
                    .eq(CourseGroup::getTermId, term.getId()).last("limit 1"));
            if (group == null) {
                group = new CourseGroup();
                group.setCourseId(course.getId());
                group.setTermId(term.getId());
                group.setLevel("L2");
                group.setLevelRequestStatus("NONE");
                group.setStatus("ACTIVE");
                groupMapper.insert(group);
            }
            CourseOffering offering = offeringMapper.selectOne(Wrappers.<CourseOffering>lambdaQuery()
                    .eq(CourseOffering::getCourseId, course.getId())
                    .eq(CourseOffering::getTermId, term.getId()).last("limit 1"));
            if (offering == null) {
                offering = new CourseOffering();
                offering.setCourseId(course.getId());
                offering.setGroupId(group.getId());
                offering.setTermId(term.getId());
                offering.setPlannedWeeks(16);
                offering.setPlannedTimesPerWeek(2);
                offering.setPlannedSessions(sessions);
                offering.setEffectiveSessions(sessions);
                offering.setEffectiveSource("SYNTHETIC");
                offering.setActualSessions(sessions);
                offering.setPassScore(60);
                offering.setStatus("ACTIVE");
                offeringMapper.insert(offering);
            }
            // 成绩构成（锁定）：平时 60（作业 40 / 测验 30 / 表现 30）+ 期末 40
            ScoreScheme scheme = schemeOf(group.getId(), term.getId());
            Map<String, ScoreSchemeItem> items = schemeItemsOf(scheme);
            int studentsPerClass = (int) Math.ceil(students.size() / (double) classesPerCourse);

            for (int k = 0; k < classesPerCourse; k++) {
                Teacher teacher = teachers.get((ci * classesPerCourse + k) % teachers.size());
                BigDecimal bias = TEACHER_BIAS[(ci * classesPerCourse + k) % TEACHER_BIAS.length];
                String classCode = "SYN" + (ci + 1) + "-" + (k + 1);
                TeachingClass tc = classMapper.selectOne(Wrappers.<TeachingClass>lambdaQuery()
                        .eq(TeachingClass::getOfferingId, offering.getId())
                        .eq(TeachingClass::getClassCode, classCode).last("limit 1"));
                if (tc == null) {
                    tc = new TeachingClass();
                    tc.setOfferingId(offering.getId());
                    tc.setTeacherId(teacher.getId());
                    tc.setClassCode(classCode);
                    tc.setClassName(courseName.replace("（一）", "").replace("（二）", "") + " 教学班 " + (k + 1));
                    tc.setCapacity(studentsPerClass);
                    tc.setIsRetakeClass(0);
                    tc.setJoinReviewEnabled(0);
                    tc.setStatus("ACTIVE");
                    classMapper.insert(tc);
                }
                classTotal++;
                // 课次
                List<CourseSession> sessionsOfClass = sessionMapper.selectList(
                        Wrappers.<CourseSession>lambdaQuery()
                                .eq(CourseSession::getTeachingClassId, tc.getId())
                                .orderByAsc(CourseSession::getSessionNo));
                if (sessionsOfClass.isEmpty()) {
                    sessionsOfClass = new ArrayList<>();
                    for (int s = 1; s <= sessions; s++) {
                        CourseSession cs = new CourseSession();
                        cs.setOfferingId(offering.getId());
                        cs.setTeachingClassId(tc.getId());
                        cs.setTermId(term.getId());
                        cs.setWeekNo((s - 1) / 2 + 1);
                        cs.setSessionNo(s);
                        cs.setSessionDate(term.getStartDate().plusDays((s - 1) / 2 * 7 + ((s - 1) % 2)));
                        cs.setSessionType("NORMAL");
                        cs.setStatus("NORMAL");
                        cs.setRollCallStatus("NOT_CALLED");
                        cs.setIsVoid(0);
                        sessionMapper.insert(cs);
                        sessionsOfClass.add(cs);
                        sessionCount++;
                    }
                }
                // 选课
                int fromIndex = k * studentsPerClass;
                int toIndex = Math.min(fromIndex + studentsPerClass, students.size());
                List<Enrollment> ens = new ArrayList<>();
                for (int si = fromIndex; si < toIndex; si++) {
                    Student st = students.get(si);
                    Enrollment e = enrollmentMapper.selectOne(Wrappers.<Enrollment>lambdaQuery()
                            .eq(Enrollment::getTeachingClassId, tc.getId())
                            .eq(Enrollment::getStudentId, st.getId()).last("limit 1"));
                    if (e == null) {
                        e = new Enrollment();
                        e.setStudentId(st.getId());
                        e.setTeachingClassId(tc.getId());
                        e.setOfferingId(offering.getId());
                        e.setTermId(term.getId());
                        e.setStatus("ENROLLED");
                        e.setRetakeTimes(0);
                        e.setEnrollTime(LocalDateTime.now());
                        enrollmentMapper.insert(e);
                        enrollments++;
                    }
                    ens.add(e);
                }
                // 考勤：前 N 次课，95% 出勤
                for (int s = 0; s < Math.min(attendanceSessions, sessionsOfClass.size()); s++) {
                    CourseSession cs = sessionsOfClass.get(s);
                    if ("CALLED".equals(cs.getRollCallStatus())) {
                        continue;
                    }
                    int present = 0;
                    for (Enrollment e : ens) {
                        int rnd = (int) Math.abs((e.getStudentId() * 31 + s * 17) % 100);
                        String status;
                        if (rnd < 82) {
                            status = "PRESENT";
                            present++;
                        } else if (rnd < 90) {
                            status = "PRESENT";
                            present++;
                        } else if (rnd < 95) {
                            status = "LATE";
                        } else if (rnd < 98) {
                            status = "ABSENT";
                        } else {
                            status = "LEAVE";
                        }
                        AttendanceRecord ar = new AttendanceRecord();
                        ar.setEnrollmentId(e.getId());
                        ar.setStudentId(e.getStudentId());
                        ar.setSessionId(cs.getId());
                        ar.setTermId(term.getId());
                        ar.setStatus(status);
                        ar.setPhase("NORMAL");
                        ar.setSource("TEACHER");
                        ar.setRecordedBy(teacher.getId());
                        ar.setRecordedRole("TEACHER");
                        ar.setRecordedAt(LocalDateTime.now());
                        ar.setBatchKey("SYNTH");
                        ar.setIsVoid(0);
                        attendanceMapper.insert(ar);
                        attendance++;
                    }
                    cs.setRollCallStatus("CALLED");
                    cs.setRollCallCount(present);
                    cs.setRollCallBy(teacher.getId());
                    cs.setRollCallTime(LocalDateTime.now());
                    sessionMapper.updateById(cs);
                }
                // 两次活动：随堂测验（测验项）+ 课后作业（作业项）
                papers += createPaper(tc, term, items.get("QUIZ"), "QUIZ", "随堂测验（合成）", 1, bias, ens, true);
                papers += createPaper(tc, term, items.get("HW"), "HOMEWORK", "课后作业（合成）", 2, bias, ens, true);
                submissions += submitAll(tc, ens, bias);
                // 手工项：课堂表现 + 期末
                scoreItems += createManualItems(tc, term, teacher, items.get("PERF"), 1, bias, ens, 82, 15);
                scoreItems += createManualItems(tc, term, teacher, items.get("FINAL"), 1, bias, ens, 55, 45);
                // 教师尺度真值（供第二阶段核对）
                styles += recordStyleTruth(term, offering, course, teacher, bias, ens.size(), items.get("PERF"));
                if (withScores) {
                    scoreResults += scoreMatrixService.commitSummary(tc.getId(), teacher.getId()).getRows().size();
                }
            }
        }
        out.setCourses(courseCount);
        out.setOfferings(courseCount);
        out.setTeachingClasses(classTotal);
        out.setEnrollments(enrollments);
        out.setSessions(sessionCount);
        out.setAttendanceRecords(attendance);
        out.setPapers(papers);
        out.setSubmissions(submissions);
        out.setScoreItems(scoreItems);
        out.setScoreResults(scoreResults);
        out.setTeacherStyles(styles);
        out.setElapsedMs(System.currentTimeMillis() - start);
        notes.add("教师尺度真值已写入 teacher_style（engine_version=SYNTH-TRUTH），可用于验证第二阶段的 Δ 估计");
        out.setNotes(notes);
        log.info("合成数据生成完成：{} 学生，{} 课程，耗时 {} ms",
                students.size(), courseCount, out.getElapsedMs());
        return out;
    }

    // ============================================================ 生成细节
    private ScoreScheme schemeOf(Long groupId, Long termId) {
        ScoreScheme scheme = schemeMapper.selectOne(Wrappers.<ScoreScheme>lambdaQuery()
                .eq(ScoreScheme::getGroupId, groupId)
                .orderByDesc(ScoreScheme::getVersion).last("limit 1"));
        if (scheme != null) {
            return scheme;
        }
        scheme = new ScoreScheme();
        scheme.setGroupId(groupId);
        scheme.setTermId(termId);
        scheme.setVersion(1);
        scheme.setLevel("L2");
        scheme.setTotalWeight(new BigDecimal("100"));
        scheme.setConfirmStatus("LOCKED");
        scheme.setConfirmTotal(1);
        scheme.setConfirmDone(1);
        scheme.setConfirmedByJson("[]");
        scheme.setLocked(1);
        scheme.setLockType("TEMPLATE_AUTO");
        scheme.setLockedAt(LocalDateTime.now());
        scheme.setRemark("合成数据：按考查课默认模板锁定");
        schemeMapper.insert(scheme);
        Long usual = insertSchemeItem(scheme.getId(), null, 1, "USUAL", "平时", "USUAL",
                new BigDecimal("60"), "AUTO", null, 1);
        insertSchemeItem(scheme.getId(), usual, 2, "HW", "课后作业", "HOMEWORK",
                new BigDecimal("40"), "AUTO", "AVG", 2);
        insertSchemeItem(scheme.getId(), usual, 2, "QUIZ", "随堂测试", "QUIZ",
                new BigDecimal("30"), "AUTO", "AVG", 3);
        insertSchemeItem(scheme.getId(), usual, 2, "PERF", "课堂表现", "PERFORMANCE",
                new BigDecimal("30"), "MANUAL", null, 4);
        insertSchemeItem(scheme.getId(), null, 1, "FINAL", "期末", "FINAL",
                new BigDecimal("40"), "MANUAL", null, 5);
        return scheme;
    }

    private Long insertSchemeItem(Long schemeId, Long parentId, int level, String code, String name,
                                  String category, BigDecimal weight, String dataSource,
                                  String aggregate, int sort) {
        ScoreSchemeItem it = new ScoreSchemeItem();
        it.setSchemeId(schemeId);
        it.setParentId(parentId);
        it.setItemLevel(level);
        it.setItemCode(code);
        it.setItemName(name);
        it.setCategory(category);
        it.setWeight(weight);
        it.setFullScore(new BigDecimal("100"));
        it.setDataSource(dataSource);
        it.setScoreType("NUMERIC");
        it.setAggregateMode(aggregate);
        it.setExpectedTimes(aggregate == null ? null : 3);
        it.setIsAnchor(0);
        it.setIsSubjective("PERFORMANCE".equals(category) ? 1 : 0);
        it.setScorerRole("TEACHER");
        it.setSortNo(sort);
        it.setStatus("ACTIVE");
        schemeItemMapper.insert(it);
        return it.getId();
    }

    private Map<String, ScoreSchemeItem> schemeItemsOf(ScoreScheme scheme) {
        Map<String, ScoreSchemeItem> out = new LinkedHashMap<>();
        for (ScoreSchemeItem it : schemeItemMapper.selectList(Wrappers.<ScoreSchemeItem>lambdaQuery()
                .eq(ScoreSchemeItem::getSchemeId, scheme.getId()))) {
            if (it.getItemCode() != null) {
                out.put(it.getItemCode(), it);
            }
        }
        return out;
    }

    private int createPaper(TeachingClass tc, Term term, ScoreSchemeItem item, String type,
                            String name, int no, BigDecimal bias, List<Enrollment> ens, boolean published) {
        if (item == null) {
            return 0;
        }
        Paper p = new Paper();
        p.setTeachingClassId(tc.getId());
        p.setOfferingId(tc.getOfferingId());
        p.setTermId(term.getId());
        p.setPaperType(type);
        p.setPaperName(name + "-" + no);
        p.setStartTime(term.getStartDate().atStartOfDay().plusDays(no * 7L));
        p.setEndTime(p.getStartTime().plusDays(7));
        p.setDurationMinutes(30);
        p.setFullScore(new BigDecimal("100"));
        p.setSchemeItemId(item.getId());
        p.setPublisherRole("TEACHER");
        p.setPublisherId(tc.getTeacherId());
        p.setAllowViewPaper(1);
        p.setShowScore(1);
        p.setShowAnswer("AFTER_MARK");
        p.setStatus(published ? "CLOSED" : "PUBLISHED");
        p.setCreateTime(LocalDateTime.now());
        paperMapper.insert(p);
        Question q = new Question();
        q.setPaperId(p.getId());
        q.setQNo(1);
        q.setQType("SHORT");
        q.setStem("合成数据：请写出你对本次课内容的理解（评分仅供联调）");
        q.setAnswerJson("{\"ref\":\"要点齐全即可\"}");
        q.setFullScore(new BigDecimal("100"));
        q.setGroupName("一、简答题");
        q.setSortNo(1);
        questionMapper.insert(q);
        return 1;
    }

    private int submitAll(TeachingClass tc, List<Enrollment> ens, BigDecimal bias) {
        List<Paper> papers = paperMapper.selectList(Wrappers.<Paper>lambdaQuery()
                .eq(Paper::getTeachingClassId, tc.getId()));
        int count = 0;
        for (Paper p : papers) {
            for (Enrollment e : ens) {
                int rnd = (int) Math.abs((e.getStudentId() * 53 + p.getPaperType().hashCode()) % 46);
                BigDecimal score = BigDecimal.valueOf(55 + rnd).add(bias);
                if (score.compareTo(BigDecimal.ZERO) < 0) {
                    score = BigDecimal.ZERO;
                }
                if (score.compareTo(new BigDecimal("100")) > 0) {
                    score = new BigDecimal("100");
                }
                Submission s = new Submission();
                s.setPaperId(p.getId());
                s.setTeachingClassId(tc.getId());
                s.setTermId(p.getTermId());
                s.setStudentId(e.getStudentId());
                s.setEnrollmentId(e.getId());
                s.setSubmitStatus("MARKED");
                s.setMarkedStatus("ALL_MARKED");
                s.setObjectiveScore(BigDecimal.ZERO);
                s.setSubjectiveScore(score);
                s.setTotalScore(score);
                s.setStartTime(p.getStartTime());
                s.setSubmitTime(p.getStartTime().plusMinutes(20));
                s.setDurationSeconds(1200);
                s.setMarkedBy(tc.getTeacherId());
                s.setMarkedAt(LocalDateTime.now());
                s.setIsAbsent(0);
                s.setRemark("合成数据");
                submissionMapper.insert(s);
                count++;
            }
        }
        return count;
    }

    private int createManualItems(TeachingClass tc, Term term, Teacher teacher, ScoreSchemeItem item,
                                  int timesNo, BigDecimal bias, List<Enrollment> ens, int base, int span) {
        if (item == null) {
            return 0;
        }
        int count = 0;
        for (Enrollment e : ens) {
            int rnd = (int) Math.abs((e.getStudentId() * 37 + item.getSortNo() * 11) % span);
            BigDecimal score = BigDecimal.valueOf(base + rnd).add(bias);
            if (score.compareTo(BigDecimal.ZERO) < 0) {
                score = BigDecimal.ZERO;
            }
            if (score.compareTo(new BigDecimal("100")) > 0) {
                score = new BigDecimal("100");
            }
            ScoreItem si = new ScoreItem();
            si.setEnrollmentId(e.getId());
            si.setStudentId(e.getStudentId());
            si.setSchemeItemId(item.getId());
            si.setTermId(term.getId());
            si.setTimesNo(timesNo);
            si.setScore(score);
            si.setFullScore(new BigDecimal("100"));
            si.setIsAbsent(0);
            si.setSource("MANUAL");
            si.setScorerId(teacher.getId());
            si.setScorerRole("TEACHER");
            si.setScoredAt(LocalDateTime.now());
            si.setIsVoid(0);
            si.setRemark("合成数据");
            scoreItemMapper.insert(si);
            count++;
        }
        return count;
    }

    private int recordStyleTruth(Term term, CourseOffering offering, Course course, Teacher teacher,
                                 BigDecimal bias, int sampleSize, ScoreSchemeItem item) {
        TeacherStyle ts = new TeacherStyle();
        ts.setTeacherId(teacher.getId());
        ts.setOfferingId(offering.getId());
        ts.setCourseId(course.getId());
        ts.setTermId(term.getId());
        ts.setSchemeItemId(item == null ? null : item.getId());
        ts.setItemCode(item == null ? null : item.getItemCode());
        ts.setRawDelta(bias);
        ts.setDelta(bias);
        ts.setShrinkLambda(BigDecimal.ONE);
        ts.setSampleSize(sampleSize);
        ts.setStdDev(new BigDecimal("8.00"));
        ts.setFused(0);
        ts.setFuseReason("合成数据：注入的真值");
        ts.setStatus("ACTIVE");
        ts.setEngineVersion("SYNTH-TRUTH");
        ts.setCalcTime(LocalDateTime.now());
        teacherStyleMapper.insert(ts);
        return 1;
    }

    // ============================================================ 清理
    @Transactional
    public void clearTerm(Long termId) {
        Term term = termMapper.selectById(termId);
        if (term == null || term.getTermCode() == null || !term.getTermCode().startsWith("SYN-")) {
            throw new BizException("只能清理 code 以 SYN- 开头的合成学期");
        }
        List<CourseOffering> offerings = offeringMapper.selectList(Wrappers.<CourseOffering>lambdaQuery()
                .eq(CourseOffering::getTermId, termId));
        List<Long> offeringIds = offerings.stream().map(CourseOffering::getId).collect(Collectors.toList());
        List<TeachingClass> classes = offeringIds.isEmpty() ? new ArrayList<>()
                : classMapper.selectList(Wrappers.<TeachingClass>lambdaQuery()
                .in(TeachingClass::getOfferingId, offeringIds));
        List<Long> classIds = classes.stream().map(TeachingClass::getId).collect(Collectors.toList());
        List<Enrollment> ens = classIds.isEmpty() ? new ArrayList<>()
                : enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .in(Enrollment::getTeachingClassId, classIds));
        List<Long> enrollmentIds = ens.stream().map(Enrollment::getId).collect(Collectors.toList());
        if (!enrollmentIds.isEmpty()) {
            attendanceMapper.delete(Wrappers.<AttendanceRecord>lambdaQuery()
                    .in(AttendanceRecord::getEnrollmentId, enrollmentIds));
            scoreItemMapper.delete(Wrappers.<ScoreItem>lambdaQuery()
                    .in(ScoreItem::getEnrollmentId, enrollmentIds));
            scoreResultMapper.delete(Wrappers.<ScoreResult>lambdaQuery()
                    .in(ScoreResult::getEnrollmentId, enrollmentIds));
        }
        if (!classIds.isEmpty()) {
            submissionMapper.delete(Wrappers.<Submission>lambdaQuery()
                    .in(Submission::getTeachingClassId, classIds));
            paperMapper.delete(Wrappers.<Paper>lambdaQuery()
                    .in(Paper::getTeachingClassId, classIds));
            sessionMapper.delete(Wrappers.<CourseSession>lambdaQuery()
                    .eq(CourseSession::getTermId, termId));
            teacherStyleMapper.delete(Wrappers.<TeacherStyle>lambdaQuery()
                    .eq(TeacherStyle::getTermId, termId));
            enrollmentMapper.delete(Wrappers.<Enrollment>lambdaQuery()
                    .in(Enrollment::getTeachingClassId, classIds));
            classMapper.delete(Wrappers.<TeachingClass>lambdaQuery()
                    .in(TeachingClass::getOfferingId, offeringIds));
        }
        if (!offeringIds.isEmpty()) {
            offeringMapper.delete(Wrappers.<CourseOffering>lambdaQuery()
                    .eq(CourseOffering::getTermId, termId));
        }
        List<CourseGroup> groups = groupMapper.selectList(Wrappers.<CourseGroup>lambdaQuery()
                .eq(CourseGroup::getTermId, termId));
        for (CourseGroup g : groups) {
            List<ScoreScheme> schemes = schemeMapper.selectList(Wrappers.<ScoreScheme>lambdaQuery()
                    .eq(ScoreScheme::getGroupId, g.getId()));
            for (ScoreScheme s : schemes) {
                schemeItemMapper.delete(Wrappers.<ScoreSchemeItem>lambdaQuery()
                        .eq(ScoreSchemeItem::getSchemeId, s.getId()));
                schemeMapper.deleteById(s.getId());
            }
            groupMapper.deleteById(g.getId());
        }
        log.info("已清理合成学期数据：termId={}", termId);
    }
}
