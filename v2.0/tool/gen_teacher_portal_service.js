/* gen_teacher_portal_service.js — 1-12 教师端：服务层 */
'use strict';
const fs = require('fs');
const path = require('path');

const BE = 'E:/JavaDEVIDEA/workspace/JavaProject/academic_risk_warning/v2.0/backend';
const J = path.join(BE, 'src/main/java/com/example/academic_risk_warning');
const P = 'com.example.academic_risk_warning';

function w(rel, content) {
  const p = path.join(J, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, content.replace(/^\n/, ''), 'utf8');
}

w('service/TeacherPortalService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.entity.SysAccount;
import ${P}.entity.AttendanceRecord;
import ${P}.entity.ClassAlertNotification;
import ${P}.entity.Course;
import ${P}.entity.CourseLesson;
import ${P}.entity.CourseOffering;
import ${P}.entity.CourseSession;
import ${P}.entity.Enrollment;
import ${P}.entity.LessonResource;
import ${P}.entity.Paper;
import ${P}.entity.ProjectSubmission;
import ${P}.entity.ProjectTask;
import ${P}.entity.Question;
import ${P}.entity.ResourceFile;
import ${P}.entity.ScoreResult;
import ${P}.entity.Student;
import ${P}.entity.Submission;
import ${P}.entity.Teacher;
import ${P}.entity.TeachingClass;
import ${P}.entity.Term;
import ${P}.mapper.AttendanceRecordMapper;
import ${P}.mapper.CourseLessonMapper;
import ${P}.mapper.CourseMapper;
import ${P}.mapper.CourseOfferingMapper;
import ${P}.mapper.CourseSessionMapper;
import ${P}.mapper.EnrollmentMapper;
import ${P}.mapper.LessonResourceMapper;
import ${P}.mapper.PaperMapper;
import ${P}.mapper.ProjectSubmissionMapper;
import ${P}.mapper.ProjectTaskMapper;
import ${P}.mapper.QuestionMapper;
import ${P}.mapper.ResourceFileMapper;
import ${P}.mapper.ScoreResultMapper;
import ${P}.mapper.StudentMapper;
import ${P}.mapper.SubmissionMapper;
import ${P}.mapper.SysAccountMapper;
import ${P}.mapper.TeacherMapper;
import ${P}.mapper.TeachingClassMapper;
import ${P}.mapper.TermMapper;
import ${P}.vo.CourseStatsVO;
import ${P}.vo.TeacherCourseCardVO;
import ${P}.vo.TeacherHomeVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
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
 * 教师端页面数据：首页（我的教学课程 + 待批改三类）、课程详情（教学班级 / 教学统计 / 课程考核）、
 * 以及教学班结课。口径：结课后该课程在学生端不再显示；待批改只统计需要人工处理的量。
 */
@Service
@RequiredArgsConstructor
public class TeacherPortalService {

    private final TeachingClassMapper classMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final TermMapper termMapper;
    private final TeacherMapper teacherMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final SysAccountMapper accountMapper;
    private final CourseSessionMapper sessionMapper;
    private final CourseLessonMapper lessonMapper;
    private final LessonResourceMapper lessonResourceMapper;
    private final ResourceFileMapper resourceFileMapper;
    private final PaperMapper paperMapper;
    private final QuestionMapper questionMapper;
    private final SubmissionMapper submissionMapper;
    private final ProjectTaskMapper projectTaskMapper;
    private final ProjectSubmissionMapper projectSubmissionMapper;
    private final AttendanceRecordMapper attendanceMapper;
    private final ScoreResultMapper scoreResultMapper;
    private final CourseContentService contentService;

    // ============================================================ 首页
    public TeacherHomeVO home(Long teacherId, Long termId) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new BizException("教师档案不存在");
        }
        List<TeachingClass> classes = classMapper.selectList(Wrappers.<TeachingClass>lambdaQuery()
                .eq(TeachingClass::getTeacherId, teacherId)
                .orderByAsc(TeachingClass::getId));
        Map<Long, CourseOffering> offerings = new LinkedHashMap<>();
        Map<Long, Long> classCountByOffering = new LinkedHashMap<>();
        List<TeachingClass> kept = new ArrayList<>();
        for (TeachingClass c : classes) {
            CourseOffering o = offeringMapper.selectById(c.getOfferingId());
            if (o == null) {
                continue;
            }
            if (termId != null && !termId.equals(o.getTermId())) {
                continue;
            }
            offerings.put(o.getId(), o);
            classCountByOffering.merge(o.getId(), 1L, Long::sum);
            kept.add(c);
        }
        Map<Long, Course> courses = new LinkedHashMap<>();
        Map<Long, Term> terms = new LinkedHashMap<>();
        for (CourseOffering o : offerings.values()) {
            Course c = courseMapper.selectById(o.getCourseId());
            if (c != null) {
                courses.put(c.getId(), c);
            }
            Term t = termMapper.selectById(o.getTermId());
            if (t != null) {
                terms.put(t.getId(), t);
            }
        }
        Map<Long, Integer> studentsByClass = enrollmentCounts(kept.stream()
                .map(TeachingClass::getId).collect(Collectors.toList()));

        Map<Long, TeacherCourseCardVO> cards = new LinkedHashMap<>();
        for (TeachingClass c : kept) {
            CourseOffering o = offerings.get(c.getOfferingId());
            if (o == null) {
                continue;
            }
            TeacherCourseCardVO card = cards.computeIfAbsent(o.getId(), k -> {
                TeacherCourseCardVO v = new TeacherCourseCardVO();
                v.setOfferingId(o.getId());
                v.setTermId(o.getTermId());
                Term t = terms.get(o.getTermId());
                v.setTermName(t == null ? null : t.getTermName());
                Course course = courses.get(o.getCourseId());
                v.setCourseName(course == null ? null : course.getCourseName());
                v.setCourseType(course == null ? null : course.getCourseType());
                v.setClassCount(classCountByOffering.getOrDefault(o.getId(), 0L).intValue());
                v.setClasses(new ArrayList<>());
                v.setClosedClassCount(0);
                return v;
            });
            TeacherCourseCardVO.Lite lite = new TeacherCourseCardVO.Lite();
            lite.setTeachingClassId(c.getId());
            lite.setClassCode(c.getClassCode());
            lite.setClassName(c.getClassName());
            lite.setStudentCount(studentsByClass.getOrDefault(c.getId(), 0));
            lite.setEffectiveSessions(o.getEffectiveSessions());
            lite.setStatus(c.getStatus());
            card.getClasses().add(lite);
            if ("CLOSED".equals(c.getStatus())) {
                card.setClosedClassCount(card.getClosedClassCount() + 1);
            }
        }
        for (TeacherCourseCardVO card : cards.values()) {
            int students = card.getClasses().stream().mapToInt(TeacherCourseCardVO.Lite::getStudentCount).sum();
            card.setStudentCount(students);
            card.setAllClosed(card.getClosedClassCount().equals(card.getClasses().size()));
        }

        TeacherHomeVO vo = new TeacherHomeVO();
        vo.setCourses(new ArrayList<>(cards.values()));
        List<TeacherHomeVO.PendingItem> homeworks = new ArrayList<>();
        List<TeacherHomeVO.PendingItem> quizzes = new ArrayList<>();
        List<TeacherHomeVO.PendingItem> projects = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (TeachingClass c : kept) {
            CourseOffering o = offerings.get(c.getOfferingId());
            Course course = o == null ? null : courses.get(o.getCourseId());
            String courseName = course == null ? "" : course.getCourseName();
            for (Paper p : paperMapper.selectList(Wrappers.<Paper>lambdaQuery()
                    .eq(Paper::getTeachingClassId, c.getId())
                    .in(Paper::getStatus, List.of("PUBLISHED", "CLOSED")))) {
                List<Submission> subs = submissionMapper.selectList(Wrappers.<Submission>lambdaQuery()
                        .eq(Submission::getPaperId, p.getId()));
                int pending = 0;
                for (Submission s : subs) {
                    if ("SYSTEM_MARKED".equals(s.getMarkedStatus())) {
                        pending++;
                    }
                }
                if (pending == 0) {
                    continue;
                }
                TeacherHomeVO.PendingItem item = new TeacherHomeVO.PendingItem();
                item.setKind("PAPER");
                item.setId(p.getId());
                item.setTeachingClassId(c.getId());
                item.setCourseName(courseName);
                item.setClassName(c.getClassName());
                item.setTitle(p.getPaperName());
                item.setPendingCount(pending);
                item.setEndTime(p.getEndTime());
                item.setFullScore(p.getFullScore());
                if ("HOMEWORK".equals(p.getPaperType())) {
                    homeworks.add(item);
                } else {
                    quizzes.add(item);
                }
            }
            for (ProjectTask t : projectTaskMapper.selectList(Wrappers.<ProjectTask>lambdaQuery()
                    .eq(ProjectTask::getTeachingClassId, c.getId())
                    .in(ProjectTask::getStatus, List.of("PUBLISHED", "CLOSED")))) {
                List<ProjectSubmission> subs = projectSubmissionMapper.selectList(
                        Wrappers.<ProjectSubmission>lambdaQuery()
                                .eq(ProjectSubmission::getProjectId, t.getId())
                                .eq(ProjectSubmission::getSubmitStatus, "SUBMITTED"));
                if (subs.isEmpty()) {
                    continue;
                }
                TeacherHomeVO.PendingItem item = new TeacherHomeVO.PendingItem();
                item.setKind("PROJECT");
                item.setId(t.getId());
                item.setTeachingClassId(c.getId());
                item.setCourseName(courseName);
                item.setClassName(c.getClassName());
                item.setTitle(t.getProjectName());
                item.setPendingCount(subs.size());
                item.setEndTime(t.getEndTime());
                item.setFullScore(t.getFullScore());
                projects.add(item);
            }
        }
        // 按截止时间临近优先
        java.util.Comparator<TeacherHomeVO.PendingItem> byEnd = java.util.Comparator.comparing(
                TeacherHomeVO.PendingItem::getEndTime,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
        homeworks.sort(byEnd);
        quizzes.sort(byEnd);
        projects.sort(byEnd);
        vo.setPendingHomeworks(homeworks);
        vo.setPendingQuizzes(quizzes);
        vo.setPendingProjects(projects);
        vo.setHomeworkPending(homeworks.stream().mapToInt(TeacherHomeVO.PendingItem::getPendingCount).sum());
        vo.setQuizPending(quizzes.stream().mapToInt(TeacherHomeVO.PendingItem::getPendingCount).sum());
        vo.setProjectPending(projects.stream().mapToInt(TeacherHomeVO.PendingItem::getPendingCount).sum());
        return vo;
    }

    // ============================================================ 教学统计
    public CourseStatsVO stats(Long teachingClassId, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(teachingClassId, teacherId);
        CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
        Course course = o == null ? null : courseMapper.selectById(o.getCourseId());
        Teacher teacher = teacherMapper.selectById(tc.getTeacherId());
        List<Enrollment> ens = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, teachingClassId));
        List<Long> studentIds = ens.stream().map(Enrollment::getStudentId).distinct().collect(Collectors.toList());

        CourseStatsVO v = new CourseStatsVO();
        v.setTeachingClassId(tc.getId());
        v.setCourseName(course == null ? null : course.getCourseName());
        v.setClassName(tc.getClassName());
        v.setTeacherName(teacher == null ? null : teacher.getTeacherName());
        v.setStudentCount(studentIds.size());
        v.setEffectiveSessions(o == null ? null : o.getEffectiveSessions());

        // 今日活跃：库里记录的最后登录时间在今天的、且属于本班的学生账号
        int active = 0;
        if (!studentIds.isEmpty()) {
            List<Long> accountIds = new ArrayList<>();
            for (Student s : studentMapper.selectBatchIds(studentIds)) {
                if (s.getAccountId() != null) {
                    accountIds.add(s.getAccountId());
                }
            }
            if (!accountIds.isEmpty()) {
                LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
                active = accountMapper.selectCount(Wrappers.<SysAccount>lambdaQuery()
                        .in(SysAccount::getId, accountIds)
                        .ge(SysAccount::getLastLoginTime, startOfDay)).intValue();
            }
        }
        v.setActiveToday(active);
        v.setActiveRate(studentIds.isEmpty() ? null
                : BigDecimal.valueOf(active * 100.0 / studentIds.size()).setScale(2, RoundingMode.HALF_UP));

        // 出勤率：（出勤 + 迟到）/ 已点名课次
        List<AttendanceRecord> records = ens.isEmpty() ? new ArrayList<>()
                : attendanceMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                .in(AttendanceRecord::getEnrollmentId, ens.stream().map(Enrollment::getId)
                        .collect(Collectors.toList()))
                .eq(AttendanceRecord::getIsVoid, 0));
        long called = sessionMapper.selectCount(Wrappers.<CourseSession>lambdaQuery()
                .eq(CourseSession::getTeachingClassId, teachingClassId)
                .eq(CourseSession::getRollCallStatus, "CALLED"));
        v.setCalledSessions((int) called);
        if (!records.isEmpty()) {
            long attended = records.stream()
                    .filter(r -> "PRESENT".equals(r.getStatus()) || "LATE".equals(r.getStatus())).count();
            v.setAttendanceRate(BigDecimal.valueOf(attended * 100.0 / records.size())
                    .setScale(2, RoundingMode.HALF_UP));
        } else {
            v.setAttendanceRate(BigDecimal.ZERO);
        }

        // 任务数
        List<Paper> papers = paperMapper.selectList(Wrappers.<Paper>lambdaQuery()
                .eq(Paper::getTeachingClassId, teachingClassId)
                .in(Paper::getStatus, List.of("PUBLISHED", "CLOSED")));
        List<ProjectTask> projects = projectTaskMapper.selectList(Wrappers.<ProjectTask>lambdaQuery()
                .eq(ProjectTask::getTeachingClassId, teachingClassId)
                .in(ProjectTask::getStatus, List.of("PUBLISHED", "CLOSED")));
        long materials = lessonResourceMapper.selectCount(Wrappers.<LessonResource>lambdaQuery()
                .eq(LessonResource::getTeachingClassId, teachingClassId)
                .eq(LessonResource::getPublishStatus, "PUBLISHED"));
        long fileLib = resourceFileMapper.selectCount(Wrappers.<ResourceFile>lambdaQuery()
                .eq(ResourceFile::getTeachingClassId, teachingClassId)
                .eq(ResourceFile::getIsVoid, 0));
        v.setMaterialCount((int) (materials + fileLib));
        v.setQuizCount((int) papers.stream().filter(p -> "QUIZ".equals(p.getPaperType())
                || "EXAM".equals(p.getPaperType())).count());
        v.setHomeworkCount((int) papers.stream().filter(p -> "HOMEWORK".equals(p.getPaperType())).count());
        v.setProjectCount(projects.size());
        LocalDateTime now = LocalDateTime.now();
        v.setNotStartedQuiz(countNotStarted(papers, now, "QUIZ"));
        v.setRunningQuiz(countRunning(papers, now, "QUIZ"));
        v.setFinishedQuiz(countFinished(papers, now, "QUIZ"));
        v.setNotStartedHomework(countNotStarted(papers, now, "HOMEWORK"));
        v.setRunningHomework(countRunning(papers, now, "HOMEWORK"));
        v.setFinishedHomework(countFinished(papers, now, "HOMEWORK"));
        int pNot = 0;
        int pRun = 0;
        int pFin = 0;
        for (ProjectTask t : projects) {
            if (t.getStartTime() != null && now.isBefore(t.getStartTime())) {
                pNot++;
            } else if (t.getEndTime() != null && now.isAfter(t.getEndTime())) {
                pFin++;
            } else {
                pRun++;
            }
        }
        v.setNotStartedProject(pNot);
        v.setRunningProject(pRun);
        v.setFinishedProject(pFin);

        // 平均成绩（已定稿）
        List<ScoreResult> results = ens.isEmpty() ? new ArrayList<>()
                : scoreResultMapper.selectList(Wrappers.<ScoreResult>lambdaQuery()
                .in(ScoreResult::getEnrollmentId, ens.stream().map(Enrollment::getId)
                        .collect(Collectors.toList()))
                .eq(ScoreResult::getIsVoid, 0)
                .eq(ScoreResult::getScoreStatus, "FINAL"));
        if (!results.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            int fails = 0;
            for (ScoreResult r : results) {
                if (r.getTotalScore() != null) {
                    sum = sum.add(r.getTotalScore());
                }
                if (r.getIsFail() != null && r.getIsFail() == 1) {
                    fails++;
                }
            }
            v.setScoredCount(results.size());
            v.setAvgScore(sum.divide(BigDecimal.valueOf(results.size()), 2, RoundingMode.HALF_UP));
            v.setPassRate(BigDecimal.valueOf((results.size() - fails) * 100.0 / results.size())
                    .setScale(2, RoundingMode.HALF_UP));
        }
        return v;
    }

    private int countNotStarted(List<Paper> papers, LocalDateTime now, String type) {
        return (int) papers.stream().filter(p -> type.equals(p.getPaperType()))
                .filter(p -> p.getStartTime() != null && now.isBefore(p.getStartTime())).count();
    }

    private int countRunning(List<Paper> papers, LocalDateTime now, String type) {
        return (int) papers.stream().filter(p -> type.equals(p.getPaperType()))
                .filter(p -> (p.getStartTime() == null || !now.isBefore(p.getStartTime()))
                        && (p.getEndTime() == null || !now.isAfter(p.getEndTime()))).count();
    }

    private int countFinished(List<Paper> papers, LocalDateTime now, String type) {
        return (int) papers.stream().filter(p -> type.equals(p.getPaperType()))
                .filter(p -> p.getEndTime() != null && now.isAfter(p.getEndTime())).count();
    }

    // ============================================================ 导出学生数据
    public String exportStudents(Long teachingClassId, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(teachingClassId, teacherId);
        List<Enrollment> ens = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, teachingClassId).orderByAsc(Enrollment::getId));
        Map<Long, Student> students = new LinkedHashMap<>();
        if (!ens.isEmpty()) {
            for (Student s : studentMapper.selectBatchIds(ens.stream().map(Enrollment::getStudentId)
                    .distinct().collect(Collectors.toList()))) {
                students.put(s.getId(), s);
            }
        }
        Map<Long, int[]> att = new LinkedHashMap<>();
        Map<Long, ScoreResult> scores = new LinkedHashMap<>();
        if (!ens.isEmpty()) {
            List<String> csv = new ArrayList<>();
            for (AttendanceRecord r : attendanceMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                    .in(AttendanceRecord::getEnrollmentId, ens.stream().map(Enrollment::getId)
                            .collect(Collectors.toList()))
                    .eq(AttendanceRecord::getIsVoid, 0))) {
                int[] c = att.computeIfAbsent(r.getStudentId(), k -> new int[]{0, 0, 0});
                if ("ABSENT".equals(r.getStatus())) {
                    c[0]++;
                } else if ("LATE".equals(r.getStatus())) {
                    c[1]++;
                } else if ("LEAVE".equals(r.getStatus())) {
                    c[2]++;
                }
            }
            for (ScoreResult r : scoreResultMapper.selectList(Wrappers.<ScoreResult>lambdaQuery()
                    .in(ScoreResult::getEnrollmentId, ens.stream().map(Enrollment::getId)
                            .collect(Collectors.toList()))
                    .eq(ScoreResult::getIsVoid, 0))) {
                scores.put(r.getEnrollmentId(), r);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("学号,姓名,缺课,迟到,请假,加权分,考勤扣分,总评,是否挂科,挂科原因,成绩状态\\n");
        for (Enrollment e : ens) {
            Student st = students.get(e.getStudentId());
            int[] c = att.getOrDefault(st == null ? -1L : st.getId(), new int[]{0, 0, 0});
            ScoreResult r = scores.get(e.getId());
            sb.append(csv(st == null ? "" : st.getStudentNo())).append(',')
                    .append(csv(st == null ? "" : st.getStudentName())).append(',')
                    .append(c[0]).append(',').append(c[1]).append(',').append(c[2]).append(',')
                    .append(r == null || r.getWeightedScore() == null ? "" : r.getWeightedScore()).append(',')
                    .append(r == null || r.getDeductionPoints() == null ? "" : r.getDeductionPoints()).append(',')
                    .append(r == null || r.getTotalScore() == null ? "" : r.getTotalScore()).append(',')
                    .append(r == null ? "" : (r.getIsFail() != null && r.getIsFail() == 1 ? "是" : "否")).append(',')
                    .append(r == null || r.getFailReason() == null ? ""
                            : ("HARD_LINE".equals(r.getFailReason()) ? "缺课超三分之一"
                            : ("SCORE".equals(r.getFailReason()) ? "总评不足" : "成绩未齐"))).append(',')
                    .append(r == null ? "未定稿" : r.getScoreStatus()).append('\\n');
        }
        return sb.toString();
    }

    private String csv(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\\"")) {
            return "\\"" + s.replace("\\"", "\\"\\"") + "\\"";
        }
        return s;
    }

    // ============================================================ 结课
    @Transactional
    public void closeClass(Long teachingClassId, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(teachingClassId, teacherId);
        if ("CLOSED".equals(tc.getStatus())) {
            throw new BizException("该教学班已经结课");
        }
        tc.setStatus("CLOSED");
        tc.setUpdateBy(teacherId);
        classMapper.updateById(tc);
    }

    private Map<Long, Integer> enrollmentCounts(List<Long> classIds) {
        Map<Long, Integer> out = new LinkedHashMap<>();
        if (classIds.isEmpty()) {
            return out;
        }
        for (Enrollment e : enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .in(Enrollment::getTeachingClassId, classIds))) {
            out.merge(e.getTeachingClassId(), 1, Integer::sum);
        }
        return out;
    }
}
`);

console.log('教师端模块：服务层生成完成');
