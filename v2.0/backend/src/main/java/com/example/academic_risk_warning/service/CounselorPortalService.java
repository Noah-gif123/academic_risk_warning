package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.InterventionSaveRequest;
import com.example.academic_risk_warning.dto.QuestionnaireSaveRequest;
import com.example.academic_risk_warning.entity.AdministrativeClass;
import com.example.academic_risk_warning.entity.AttendanceRecord;
import com.example.academic_risk_warning.entity.Counselor;
import com.example.academic_risk_warning.entity.CounselorClass;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.InterventionRecord;
import com.example.academic_risk_warning.entity.Major;
import com.example.academic_risk_warning.entity.Notification;
import com.example.academic_risk_warning.entity.NotificationReceipt;
import com.example.academic_risk_warning.entity.Questionnaire;
import com.example.academic_risk_warning.entity.RiskState;
import com.example.academic_risk_warning.entity.ScoreResult;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.SysAccount;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.entity.Term;
import com.example.academic_risk_warning.mapper.AdministrativeClassMapper;
import com.example.academic_risk_warning.mapper.AttendanceRecordMapper;
import com.example.academic_risk_warning.mapper.CounselorClassMapper;
import com.example.academic_risk_warning.mapper.CounselorMapper;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.InterventionRecordMapper;
import com.example.academic_risk_warning.mapper.MajorMapper;
import com.example.academic_risk_warning.mapper.NotificationMapper;
import com.example.academic_risk_warning.mapper.NotificationReceiptMapper;
import com.example.academic_risk_warning.mapper.QuestionnaireMapper;
import com.example.academic_risk_warning.mapper.RiskStateMapper;
import com.example.academic_risk_warning.mapper.ScoreResultMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.SysAccountMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.mapper.TermMapper;
import com.example.academic_risk_warning.vo.CounselorClassSummaryVO;
import com.example.academic_risk_warning.vo.CounselorNoticeVO;
import com.example.academic_risk_warning.vo.CounselorOverviewVO;
import com.example.academic_risk_warning.vo.CounselorProfileVO;
import com.example.academic_risk_warning.vo.CounselorStudentDetailVO;
import com.example.academic_risk_warning.vo.CounselorStudentVO;
import com.example.academic_risk_warning.vo.QuestionnaireVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 导员端：名下行政班的看板、学生名单与单科详情、干预记录、问卷发布、通知回执。
 * 口径：① 只看本人名下行政班；② 不展示无风险学生（看板只列有风险的学生）；
 * ③ 班集体提醒从第 5 周起：≥30% 且 ≥5 人建议班会，≥50% 且 ≥10 人升级为重点关注班级并抄送教务；
 * ④ 预警等级由第二阶段引擎产出，本阶段按**确定性指标**（挂科 / 缺课超 1/3）估算并在界面注明。
 */
@Service
@RequiredArgsConstructor
public class CounselorPortalService {

    /** 班集体提醒的起始周次（前 4 周只做个体提醒） */
    private static final int CLASS_ALERT_START_WEEK = 5;

    private final CounselorMapper counselorMapper;
    private final CounselorClassMapper counselorClassMapper;
    private final AdministrativeClassMapper adminClassMapper;
    private final MajorMapper majorMapper;
    private final StudentMapper studentMapper;
    private final SysAccountMapper accountMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final TeachingClassMapper classMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final TermMapper termMapper;
    private final ScoreResultMapper scoreResultMapper;
    private final AttendanceRecordMapper attendanceMapper;
    private final RiskStateMapper riskStateMapper;
    private final InterventionRecordMapper interventionMapper;
    private final QuestionnaireMapper questionnaireMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationReceiptMapper receiptMapper;
    private final AuditService auditService;

    // ============================================================ 基础信息
    private Counselor me(Long counselorId) {
        Counselor c = counselorMapper.selectById(counselorId);
        if (c == null) {
            throw new BizException("导员档案不存在");
        }
        return c;
    }

    public CounselorProfileVO profile(Long counselorId) {
        Counselor c = me(counselorId);
        CounselorProfileVO v = new CounselorProfileVO();
        v.setCounselorId(c.getId());
        v.setCounselorNo(c.getCounselorNo());
        v.setCounselorName(c.getCounselorName());
        v.setCollegeName(c.getCollegeName());
        v.setPhone(c.getPhone());
        v.setEmail(c.getEmail());
        if (c.getAccountId() != null) {
            SysAccount a = accountMapper.selectById(c.getAccountId());
            v.setAccountNo(a == null ? null : a.getLoginNo());
        }
        v.setClassCount(myClasses(c.getId(), null).size());
        return v;
    }

    public List<Term> terms() {
        return termMapper.selectList(Wrappers.<Term>lambdaQuery().orderByDesc(Term::getStartDate));
    }

    /** 本人名下行政班（按学期） */
    private List<AdministrativeClass> myClasses(Long counselorId, Long termId) {
        List<CounselorClass> links = counselorClassMapper.selectList(Wrappers.<CounselorClass>lambdaQuery()
                .eq(CounselorClass::getCounselorId, counselorId)
                .eq(termId != null, CounselorClass::getTermId, termId));
        // 学期没配到分配记录时，退回「该导员的全部行政班」，避免页面空白
        if (links.isEmpty() && termId != null) {
            links = counselorClassMapper.selectList(Wrappers.<CounselorClass>lambdaQuery()
                    .eq(CounselorClass::getCounselorId, counselorId));
        }
        List<Long> ids = links.stream().map(CounselorClass::getAdminClassId).distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        return adminClassMapper.selectBatchIds(ids);
    }

    private Term termOf(Long termId) {
        Term t = termId == null ? null : termMapper.selectById(termId);
        if (t == null) {
            t = termMapper.selectOne(Wrappers.<Term>lambdaQuery().eq(Term::getIsCurrent, 1).last("limit 1"));
        }
        return t;
    }

    private int weekOf(Term term) {
        if (term == null || term.getStartDate() == null) {
            return 1;
        }
        long days = ChronoUnit.DAYS.between(term.getStartDate(), LocalDate.now());
        if (days < 0) {
            return 1;
        }
        return (int) (days / 7) + 1;
    }

    // ============================================================ 看板
    public CounselorOverviewVO overview(Long counselorId, Long termId) {
        Counselor c = me(counselorId);
        Term term = termOf(termId);
        Long useTerm = term == null ? null : term.getId();
        List<AdministrativeClass> classes = myClasses(c.getId(), useTerm);
        CounselorOverviewVO v = new CounselorOverviewVO();
        v.setTermName(term == null ? null : term.getTermName());
        int week = weekOf(term);
        v.setWeekNo(week);
        v.setClassCount(classes.size());

        List<CounselorClassSummaryVO> classVOs = new ArrayList<>();
        List<CounselorStudentVO> allStudents = new ArrayList<>();
        List<String> reminders = new ArrayList<>();
        for (AdministrativeClass ac : classes) {
            List<Student> students = studentsOfClass(ac.getId());
            List<CounselorStudentVO> rows = studentVOs(students, useTerm);
            allStudents.addAll(rows);
            CounselorClassSummaryVO cv = classVO(ac, rows, week);
            classVOs.add(cv);
            if (cv.getStudentCount() > 0) {
                allStudents.size();
            }
            if ("SUGGEST".equals(cv.getClassAlertLevel())) {
                reminders.add(ac.getClassName() + "：" + cv.getClassAlertText());
            } else if ("FOCUS".equals(cv.getClassAlertLevel())) {
                reminders.add(ac.getClassName() + "：" + cv.getClassAlertText() + "（已抄送教务）");
            }
        }
        v.setClasses(classVOs);
        v.setStudentCount(allStudents.size());
        List<CounselorStudentVO> risky = allStudents.stream()
                .filter(s -> s.getRiskCourseCount() != null && s.getRiskCourseCount() > 0)
                .sorted((a, b) -> b.getRiskScore().compareTo(a.getRiskScore()))
                .collect(Collectors.toList());
        v.setRiskStudentCount(risky.size());
        v.setFailStudentCount((int) allStudents.stream()
                .filter(s -> s.getFailCourseCount() != null && s.getFailCourseCount() > 0).count());
        v.setHardFailStudentCount((int) allStudents.stream()
                .filter(s -> s.getHardFailCourseCount() != null && s.getHardFailCourseCount() > 0).count());
        if (!classVOs.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            int n = 0;
            for (CounselorClassSummaryVO cv : classVOs) {
                if (cv.getFailRate() != null) {
                    sum = sum.add(cv.getFailRate());
                    n++;
                }
            }
            v.setAvgFailRate(n == 0 ? null : sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP));
        }
        // 重点关注：有风险的课程数 ≥ 3
        v.setFocusStudents(risky.stream()
                .filter(s -> s.getRiskCourseCount() >= 3)
                .collect(Collectors.toList()));
        if (week < CLASS_ALERT_START_WEEK) {
            reminders.add("第 " + week + " 周：班集体提醒从第 " + CLASS_ALERT_START_WEEK + " 周起生效，当前只做个体提醒");
        }
        if (reminders.isEmpty()) {
            reminders.add("本周没有需要集体提醒的班级");
        }
        v.setReminders(reminders);
        return v;
    }

    private CounselorClassSummaryVO classVO(AdministrativeClass ac, List<CounselorStudentVO> rows, int week) {
        CounselorClassSummaryVO v = new CounselorClassSummaryVO();
        v.setAdminClassId(ac.getId());
        v.setClassCode(ac.getClassCode());
        v.setClassName(ac.getClassName());
        v.setGradeYear(ac.getGradeYear());
        if (ac.getMajorId() != null) {
            Major m = majorMapper.selectById(ac.getMajorId());
            v.setMajorName(m == null ? null : m.getMajorName());
        }
        v.setStudentCount(rows.size());
        int scored = 0;
        int fails = 0;
        int hard = 0;
        for (CounselorStudentVO r : rows) {
            if (r.getCourseCount() != null && r.getCourseCount() > 0) {
                scored++;
            }
            if (r.getFailCourseCount() != null && r.getFailCourseCount() > 0) {
                fails++;
            }
            if (r.getHardFailCourseCount() != null && r.getHardFailCourseCount() > 0) {
                hard++;
            }
        }
        v.setScoredCount(scored);
        v.setFailCount(fails);
        v.setHardFailCount(hard);
        v.setFailRate(scored == 0 ? null
                : BigDecimal.valueOf(fails * 100.0 / scored).setScale(2, RoundingMode.HALF_UP));
        int risky = (int) rows.stream().filter(r -> r.getRiskCourseCount() != null && r.getRiskCourseCount() > 0).count();
        int absentOver = (int) rows.stream().filter(r -> r.getHardFailCourseCount() != null
                && r.getHardFailCourseCount() > 0).count();
        v.setRiskStudentCount(risky);
        v.setAbsentOverCount(absentOver);
        v.setWeekNo(week);
        double rate = rows.isEmpty() ? 0 : risky * 100.0 / rows.size();
        if (week >= CLASS_ALERT_START_WEEK && risky >= 10 && rate >= 50) {
            v.setClassAlertLevel("FOCUS");
            v.setClassAlertText("预警（风险）学生 " + risky + " 人、占 " + Math.round(rate)
                    + "%，达到重点班级标准，建议升级并抄送教务");
        } else if (week >= CLASS_ALERT_START_WEEK && risky >= 5 && rate >= 30) {
            v.setClassAlertLevel("SUGGEST");
            v.setClassAlertText("预警（风险）学生 " + risky + " 人、占 " + Math.round(rate)
                    + "%，建议召开班会或集体谈话");
        } else {
            v.setClassAlertLevel("NONE");
            v.setClassAlertText("正常");
        }
        return v;
    }

    // ============================================================ 学生名单
    private List<Student> studentsOfClass(Long adminClassId) {
        return studentMapper.selectList(Wrappers.<Student>lambdaQuery()
                .eq(Student::getAdminClassId, adminClassId)
                .orderByAsc(Student::getStudentNo));
    }

    private List<CounselorStudentVO> studentVOs(List<Student> students, Long termId) {
        List<CounselorStudentVO> out = new ArrayList<>();
        if (students.isEmpty()) {
            return out;
        }
        List<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toList());
        Map<Long, List<Enrollment>> enrollmentByStudent = new LinkedHashMap<>();
        // 只统计当前学期的选课与成绩：否则上学期/合成学期的挂科会串到本学期的风险人数里
        List<Enrollment> ens = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .in(Enrollment::getStudentId, studentIds)
                .eq(termId != null, Enrollment::getTermId, termId));
        for (Enrollment e : ens) {
            enrollmentByStudent.computeIfAbsent(e.getStudentId(), k -> new ArrayList<>()).add(e);
        }
        Map<Long, ScoreResult> resultByEnrollment = new LinkedHashMap<>();
        Map<Long, Long> classByEnrollment = new LinkedHashMap<>();
        Map<Long, BigDecimal> allowedByClass = new LinkedHashMap<>();
        Map<Long, int[]> absentByStudent = new LinkedHashMap<>();
        if (!ens.isEmpty()) {
            for (ScoreResult r : scoreResultMapper.selectList(Wrappers.<ScoreResult>lambdaQuery()
                    .in(ScoreResult::getEnrollmentId, ens.stream().map(Enrollment::getId)
                            .collect(Collectors.toList()))
                    .eq(ScoreResult::getIsVoid, 0))) {
                resultByEnrollment.put(r.getEnrollmentId(), r);
            }
            for (Enrollment e : ens) {
                classByEnrollment.put(e.getId(), e.getTeachingClassId());
            }
            for (AttendanceRecord r : attendanceMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                    .in(AttendanceRecord::getEnrollmentId, ens.stream().map(Enrollment::getId)
                            .collect(Collectors.toList()))
                    .eq(AttendanceRecord::getIsVoid, 0))) {
                int[] c = absentByStudent.computeIfAbsent(r.getStudentId(), k -> new int[]{0, 0, 0});
                if ("ABSENT".equals(r.getStatus())) {
                    c[0]++;
                } else if ("LATE".equals(r.getStatus())) {
                    c[1]++;
                } else if ("LEAVE".equals(r.getStatus())) {
                    c[2]++;
                }
            }
        }
        // 干预记录（最新一条）
        Map<Long, InterventionRecord> latest = new LinkedHashMap<>();
        for (InterventionRecord ir : interventionMapper.selectList(Wrappers.<InterventionRecord>lambdaQuery()
                .in(InterventionRecord::getStudentId, studentIds)
                .orderByDesc(InterventionRecord::getId))) {
            latest.putIfAbsent(ir.getStudentId(), ir);
        }
        for (Student st : students) {
            CounselorStudentVO v = new CounselorStudentVO();
            v.setStudentId(st.getId());
            v.setStudentNo(st.getStudentNo());
            v.setStudentName(st.getStudentName());
            v.setAdminClassId(st.getAdminClassId());
            AdministrativeClass ac = st.getAdminClassId() == null ? null
                    : adminClassMapper.selectById(st.getAdminClassId());
            v.setAdminClassName(classLabel(ac));
            int[] att = absentByStudent.getOrDefault(st.getId(), new int[]{0, 0, 0});
            v.setAbsentTotal(att[0]);
            List<ScoreResult> mine = new ArrayList<>();
            for (Enrollment e : enrollmentByStudent.getOrDefault(st.getId(), new ArrayList<>())) {
                ScoreResult r = resultByEnrollment.get(e.getId());
                if (r != null) {
                    mine.add(r);
                }
            }
            int fails = (int) mine.stream().filter(r -> r.getIsFail() != null && r.getIsFail() == 1).count();
            int hards = (int) mine.stream().filter(r -> r.getHardFail() != null && r.getHardFail() == 1).count();
            BigDecimal min = null;
            BigDecimal sum = BigDecimal.ZERO;
            for (ScoreResult r : mine) {
                if (r.getTotalScore() == null) {
                    continue;
                }
                sum = sum.add(r.getTotalScore());
                min = min == null || r.getTotalScore().compareTo(min) < 0 ? r.getTotalScore() : min;
            }
            v.setCourseCount(mine.size());
            v.setFailCourseCount(fails);
            v.setHardFailCourseCount(hards);
            v.setRiskCourseCount(fails + hards);
            v.setMinTotalScore(min);
            v.setAvgTotalScore(mine.isEmpty() ? null
                    : sum.divide(BigDecimal.valueOf(mine.size()), 2, RoundingMode.HALF_UP));
            // 危险度：硬门槛 40 分/门 + 分数不足 20 分/门 + 缺课 2 分/次，最高按 100 截断
            BigDecimal risk = BigDecimal.valueOf(hards * 40.0 + (fails - hards) * 20.0 + att[0] * 2.0);
            v.setRiskScore(risk.min(BigDecimal.valueOf(100)));
            StringBuilder hint = new StringBuilder();
            if (hards > 0) {
                hint.append("缺课超 1/3：").append(hards).append(" 门；");
            }
            if (fails > 0) {
                hint.append("挂科：").append(fails).append(" 门；");
            }
            if (att[0] > 0) {
                hint.append("累计缺课 ").append(att[0]).append(" 次");
            }
            v.setRiskHint(hint.length() == 0 ? "暂无风险" : hint.toString());
            InterventionRecord ir = latest.get(st.getId());
            if (ir != null) {
                v.setLatestInterventionId(ir.getId());
                v.setLatestInterventionText(ir.getActionType() + "：" + (ir.getContent() == null ? "" : ir.getContent()));
            }
            out.add(v);
        }
        out.sort((a, b) -> b.getRiskScore().compareTo(a.getRiskScore()));
        return out;
    }

    public List<CounselorStudentVO> students(Long counselorId, Long adminClassId, Long termId) {
        Counselor c = me(counselorId);
        Term term = termOf(termId);
        List<AdministrativeClass> mine = myClasses(c.getId(), term == null ? null : term.getId());
        List<AdministrativeClass> target = new ArrayList<>();
        for (AdministrativeClass ac : mine) {
            if (adminClassId == null || adminClassId.equals(ac.getId())) {
                target.add(ac);
            }
        }
        if (adminClassId != null && target.isEmpty()) {
            throw new BizException("只能查看本人名下行政班的学生");
        }
        List<Student> students = new ArrayList<>();
        for (AdministrativeClass ac : target) {
            students.addAll(studentsOfClass(ac.getId()));
        }
        return studentVOs(students, term == null ? null : term.getId());
    }

    // ============================================================ 单科详情
    public CounselorStudentDetailVO studentDetail(Long counselorId, Long studentId) {
        Counselor c = me(counselorId);
        Student st = studentMapper.selectById(studentId);
        if (st == null) {
            throw new BizException("学生不存在");
        }
        boolean mine = false;
        for (AdministrativeClass ac : myClasses(c.getId(), null)) {
            if (ac.getId().equals(st.getAdminClassId())) {
                mine = true;
                break;
            }
        }
        if (!mine) {
            throw new BizException("只能查看本人名下行政班的学生");
        }
        CounselorStudentDetailVO v = new CounselorStudentDetailVO();
        v.setStudentId(st.getId());
        v.setStudentNo(st.getStudentNo());
        v.setStudentName(st.getStudentName());
        v.setEnrollYear(st.getEnrollYear());
        v.setPhone(st.getPhone());
        if (st.getMajorId() != null) {
            Major m = majorMapper.selectById(st.getMajorId());
            v.setMajorName(m == null ? null : m.getMajorName());
        }
        if (st.getAdminClassId() != null) {
            AdministrativeClass ac = adminClassMapper.selectById(st.getAdminClassId());
            v.setAdminClassName(classLabel(ac));
        }
        List<Enrollment> ens = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getStudentId, studentId));
        List<CounselorStudentDetailVO.CourseRow> rows = new ArrayList<>();
        for (Enrollment e : ens) {
            TeachingClass tc = classMapper.selectById(e.getTeachingClassId());
            if (tc == null) {
                continue;
            }
            CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
            Course course = o == null ? null : courseMapper.selectById(o.getCourseId());
            Teacher teacher = teacherMapper.selectById(tc.getTeacherId());
            ScoreResult r = scoreResultMapper.selectOne(Wrappers.<ScoreResult>lambdaQuery()
                    .eq(ScoreResult::getEnrollmentId, e.getId())
                    .eq(ScoreResult::getIsVoid, 0).last("limit 1"));
            RiskState rs = riskStateMapper.selectOne(Wrappers.<RiskState>lambdaQuery()
                    .eq(RiskState::getEnrollmentId, e.getId())
                    .eq(RiskState::getIsVoid, 0).last("limit 1"));
            CounselorStudentDetailVO.CourseRow row = new CounselorStudentDetailVO.CourseRow();
            row.setTeachingClassId(tc.getId());
            row.setCourseName(course == null ? null : course.getCourseName());
            row.setTeacherName(teacher == null ? null : teacher.getTeacherName());
            row.setEffectiveSessions(o == null ? null : o.getEffectiveSessions());
            if (r != null) {
                row.setAllowedAbsent(r.getAllowedAbsent());
                row.setAbsentCount(r.getAbsentCount());
                row.setLateCount(r.getLateCount());
                row.setLeaveCount(r.getLeaveCount());
                row.setDeductPoints(r.getDeductionPoints() == null ? null : r.getDeductionPoints().intValue());
                row.setWeightedScore(r.getWeightedScore());
                row.setTotalScore(r.getTotalScore());
                row.setPassScore(r.getPassScore());
                row.setHardFail(r.getHardFail() != null && r.getHardFail() == 1);
                row.setFail(r.getIsFail() != null && r.getIsFail() == 1);
                row.setFailReasonText(switch (r.getFailReason() == null ? "" : r.getFailReason()) {
                    case "HARD_LINE" -> "缺课超过 1/3，硬门槛挂科";
                    case "SCORE" -> "总评低于及格线";
                    case "NO_SCORE" -> "成绩未齐";
                    default -> null;
                });
                row.setScoreStatus(r.getScoreStatus());
            } else {
                row.setHardFail(false);
                row.setFail(false);
                row.setScoreStatus("NO_SCORE");
            }
            if (rs != null) {
                row.setRiskLevel(rs.getRiskLevel());
                row.setRiskHint("引擎 " + rs.getEngineVersion() + " 判定");
            } else if (Boolean.TRUE.equals(row.getHardFail()) || Boolean.TRUE.equals(row.getFail())) {
                row.setRiskLevel("HIGH");
                row.setRiskHint("按确定性指标估算（预警引擎第二阶段启用）");
            }
            rows.add(row);
        }
        v.setCourses(rows);

        List<CounselorStudentDetailVO.InterventionRow> list = new ArrayList<>();
        for (InterventionRecord ir : interventionMapper.selectList(Wrappers.<InterventionRecord>lambdaQuery()
                .eq(InterventionRecord::getStudentId, studentId)
                .eq(InterventionRecord::getIsVoid, 0)
                .orderByDesc(InterventionRecord::getId))) {
            CounselorStudentDetailVO.InterventionRow row = new CounselorStudentDetailVO.InterventionRow();
            row.setId(ir.getId());
            row.setActionType(ir.getActionType());
            row.setActionTypeText(switch (ir.getActionType() == null ? "" : ir.getActionType()) {
                case "TALK" -> "谈话";
                case "MEETING" -> "班会";
                case "PARENT" -> "联系家长";
                default -> "其他";
            });
            row.setContent(ir.getContent());
            row.setResult(ir.getResult());
            row.setEffective(ir.getEffective());
            row.setEffectiveText(switch (ir.getEffective() == null ? "" : ir.getEffective()) {
                case "EFFECTIVE" -> "有效";
                case "INEFFECTIVE" -> "无效";
                case "PENDING" -> "待复评";
                case "TERMINATED" -> "已终止";
                default -> ir.getEffective();
            });
            row.setOperatorRole(ir.getOperatorRole());
            row.setReferStatus(ir.getReferStatus());
            row.setReferNote(ir.getReferNote());
            row.setAdminFeedback(ir.getAdminFeedback());
            row.setCreateTime(ir.getCreateTime());
            list.add(row);
        }
        v.setInterventions(list);
        return v;
    }

    // ============================================================ 干预记录
    public List<CounselorStudentDetailVO.InterventionRow> interventions(Long counselorId, Long studentId) {
        return studentDetail(counselorId, studentId).getInterventions();
    }

    /** 本人名下学生的全部处理记录（干预记录页） */
    public List<Map<String, Object>> allInterventions(Long counselorId, Long adminClassId) {
        Counselor c = me(counselorId);
        List<Student> students = new ArrayList<>();
        for (AdministrativeClass ac : myClasses(c.getId(), null)) {
            if (adminClassId == null || adminClassId.equals(ac.getId())) {
                students.addAll(studentsOfClass(ac.getId()));
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        if (students.isEmpty()) {
            return out;
        }
        Map<Long, Student> byId = students.stream()
                .collect(Collectors.toMap(Student::getId, s -> s, (a, b) -> a));
        for (InterventionRecord ir : interventionMapper.selectList(Wrappers.<InterventionRecord>lambdaQuery()
                .in(InterventionRecord::getStudentId, byId.keySet())
                .eq(InterventionRecord::getIsVoid, 0)
                .orderByDesc(InterventionRecord::getId))) {
            Student st = byId.get(ir.getStudentId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", ir.getId());
            m.put("studentId", ir.getStudentId());
            m.put("studentNo", st == null ? null : st.getStudentNo());
            m.put("studentName", st == null ? null : st.getStudentName());
            m.put("adminClassId", st == null ? null : st.getAdminClassId());
            m.put("actionType", ir.getActionType());
            m.put("actionTypeText", switch (ir.getActionType() == null ? "" : ir.getActionType()) {
                case "TALK" -> "谈话";
                case "MEETING" -> "班会";
                case "PARENT" -> "联系家长";
                default -> "其他";
            });
            m.put("content", ir.getContent());
            m.put("result", ir.getResult());
            m.put("effective", ir.getEffective());
            m.put("effectiveText", switch (ir.getEffective() == null ? "" : ir.getEffective()) {
                case "EFFECTIVE" -> "有效";
                case "INEFFECTIVE" -> "无效";
                case "PENDING" -> "待复评";
                case "TERMINATED" -> "已终止";
                default -> ir.getEffective();
            });
            m.put("referStatus", ir.getReferStatus());
            m.put("referNote", ir.getReferNote());
            m.put("createTime", ir.getCreateTime());
            out.add(m);
        }
        return out;
    }

    @Transactional
    public Long addIntervention(Long counselorId, InterventionSaveRequest req) {
        Counselor c = me(counselorId);
        Student st = studentMapper.selectById(req.getStudentId());
        if (st == null) {
            throw new BizException("学生不存在");
        }
        Term term = termOf(null);
        InterventionRecord ir = new InterventionRecord();
        ir.setStudentId(req.getStudentId());
        ir.setTermId(term == null ? null : term.getId());
        ir.setStage("STAGE1");
        ir.setOperatorId(c.getId());
        ir.setOperatorRole("COUNSELOR");
        ir.setActionType(req.getActionType() == null ? "TALK" : req.getActionType());
        ir.setContent(req.getContent());
        ir.setResult(req.getResult());
        ir.setEffective(req.getEffective() == null ? "PENDING" : req.getEffective());
        ir.setEffectCheckTime(LocalDateTime.now().plusDays(14));
        ir.setReferStatus("NONE");
        ir.setIsVoid(0);
        ir.setRemark(req.getRemark());
        interventionMapper.insert(ir);
        auditService.record("INTERVENTION", ir.getId(), "CREATE", null, null,
                "导员记录干预：" + st.getStudentName() + "（" + ir.getActionType() + "）");
        return ir.getId();
    }

    @Transactional
    public void referIntervention(Long counselorId, Long id, String note) {
        me(counselorId);
        InterventionRecord ir = interventionMapper.selectById(id);
        if (ir == null) {
            throw new BizException("干预记录不存在");
        }
        if ("REFERRED".equals(ir.getReferStatus())) {
            throw new BizException("已经上报过，等待教务处理");
        }
        // 口径：两次处理仍无改善才提示可上报，这里由导员确认后执行
        long ineffective = interventionMapper.selectCount(Wrappers.<InterventionRecord>lambdaQuery()
                .eq(InterventionRecord::getStudentId, ir.getStudentId())
                .eq(InterventionRecord::getEffective, "INEFFECTIVE")
                .eq(InterventionRecord::getIsVoid, 0));
        if (ineffective < 2) {
            throw new BizException("需要两次处理均无效后才能上报（当前无效 " + ineffective + " 次）");
        }
        ir.setReferStatus("REFERRED");
        ir.setReferBy(counselorId);
        ir.setReferTime(LocalDateTime.now());
        ir.setReferNote(note);
        interventionMapper.updateById(ir);
        auditService.record("INTERVENTION", id, "REFER", null, null, "上报教务：" + note);
    }

    @Transactional
    public void terminateIntervention(Long counselorId, Long id, String reason) {
        me(counselorId);
        InterventionRecord ir = interventionMapper.selectById(id);
        if (ir == null) {
            throw new BizException("干预记录不存在");
        }
        ir.setEffective("TERMINATED");
        ir.setRemark((ir.getRemark() == null ? "" : ir.getRemark() + "；") + "终止原因：" + reason);
        interventionMapper.updateById(ir);
        auditService.record("INTERVENTION", id, "TERMINATE", null, null, "终止干预：" + reason);
    }

    // ============================================================ 问卷
    public List<QuestionnaireVO> questionnaires(Long counselorId, Long termId) {
        Counselor c = me(counselorId);
        List<Questionnaire> list = questionnaireMapper.selectList(Wrappers.<Questionnaire>lambdaQuery()
                .eq(Questionnaire::getCollectorType, "COUNSELOR")
                .eq(Questionnaire::getCollectorId, c.getId())
                .eq(termId != null, Questionnaire::getTermId, termId)
                .orderByDesc(Questionnaire::getId));
        List<QuestionnaireVO> out = new ArrayList<>();
        for (Questionnaire q : list) {
            QuestionnaireVO v = new QuestionnaireVO();
            v.setId(q.getId());
            v.setTitle(q.getTitle());
            v.setQuestionnaireType(q.getQuestionnaireType());
            v.setQuestionnaireTypeText("TEACHER_STYLE".equals(q.getQuestionnaireType())
                    ? "教师风格判定" : q.getQuestionnaireType());
            v.setAnonymous(q.getIsAnonymous() != null && q.getIsAnonymous() == 1);
            v.setStatus(q.getStatus());
            v.setStatusText(switch (q.getStatus() == null ? "" : q.getStatus()) {
                case "DRAFT" -> "草稿";
                case "COLLECTING" -> "收集中";
                case "SUBMITTED" -> "已交教务";
                case "DISTRIBUTED" -> "已分发";
                case "CLOSED" -> "已结束";
                default -> q.getStatus();
            });
            v.setAnswerCount(q.getAnswerCount());
            v.setOpenTime(q.getOpenTime());
            v.setCloseTime(q.getCloseTime());
            v.setCollectorType(q.getCollectorType());
            v.setCreateTime(q.getCreateTime());
            if (q.getCourseId() != null) {
                Course course = courseMapper.selectById(q.getCourseId());
                v.setCourseName(course == null ? null : course.getCourseName());
            }
            String classIds = q.getCollectorClassIdsJson();
            if (classIds != null) {
                for (String part : classIds.replace("[", "").replace("]", "").split(",")) {
                    String t = part.trim();
                    if (t.isEmpty()) {
                        continue;
                    }
                    try {
                        AdministrativeClass ac = adminClassMapper.selectById(Long.parseLong(t));
                        if (ac != null) {
                            v.setAdminClassId(ac.getId());
                            v.setAdminClassName(classLabel(ac));
                        }
                    } catch (NumberFormatException ignored) {
                        // 忽略脏数据
                    }
                }
            }
            out.add(v);
        }
        return out;
    }

    /** 发布「教师风格判定」问卷（匿名，按行政班） */
    @Transactional
    public QuestionnaireVO publishQuestionnaire(Long counselorId, QuestionnaireSaveRequest req) {
        Counselor c = me(counselorId);
        boolean mine = false;
        for (AdministrativeClass ac : myClasses(c.getId(), null)) {
            if (ac.getId().equals(req.getAdminClassId())) {
                mine = true;
                break;
            }
        }
        if (!mine) {
            throw new BizException("只能对本人民下行政班发布问卷");
        }
        if (req.getOfferingId() == null) {
            throw new BizException("请选择问卷对应的开课（教师风格按课程评价）");
        }
        CourseOffering offering = offeringMapper.selectById(req.getOfferingId());
        if (offering == null) {
            throw new BizException("开课实例不存在");
        }
        Questionnaire q = new Questionnaire();
        q.setOfferingId(offering.getId());
        q.setCourseId(offering.getCourseId());
        q.setTermId(offering.getTermId());
        q.setQuestionnaireType("TEACHER_STYLE");
        q.setTitle(req.getTitle() == null || req.getTitle().isBlank()
                ? "教师风格判定问卷" : req.getTitle());
        q.setQuestionJson(questionJson());
        q.setIsAnonymous(1);
        q.setCollectorType("COUNSELOR");
        q.setCollectorId(c.getId());
        q.setCollectorClassIdsJson("[" + req.getAdminClassId() + "]");
        q.setOpenTime(LocalDateTime.now());
        q.setCloseTime(LocalDateTime.now().plusDays(14));
        q.setStatus("COLLECTING");
        q.setAnswerCount(0);
        questionnaireMapper.insert(q);
        auditService.record("QUESTIONNAIRE", q.getId(), "PUBLISH", null, null,
                "导员发布教师风格判定问卷（匿名，行政班 " + req.getAdminClassId() + "）");
        List<QuestionnaireVO> all = questionnaires(counselorId, null);
        return all.stream().filter(x -> x.getId().equals(q.getId())).findFirst().orElse(null);
    }

    /** 行政班显示名：带上年级，避免「计算机1班」重名（2025 级与 2024 级） */
    private String classLabel(AdministrativeClass ac) {
        if (ac == null) {
            return null;
        }
        return ac.getGradeYear() == null ? ac.getClassName()
                : ac.getClassName() + "（" + ac.getGradeYear() + "级）";
    }

    /** 教师风格判定问卷的固定 5 题（匿名，1-5 分） */
    private String questionJson() {
        return "[{\"itemCode\":\"Q1\",\"q\":\"该教师讲课节奏是否合适\",\"type\":\"SCALE\"},"
                + "{\"itemCode\":\"Q2\",\"q\":\"作业量是否合理\",\"type\":\"SCALE\"},"
                + "{\"itemCode\":\"Q3\",\"q\":\"给分是否严格\",\"type\":\"SCALE\"},"
                + "{\"itemCode\":\"Q4\",\"q\":\"课堂互动是否充分\",\"type\":\"SCALE\"},"
                + "{\"itemCode\":\"Q5\",\"q\":\"整体评价\",\"type\":\"SCALE\"}]";
    }

    /** 本学期开课列表（发布问卷时选择课程用） */
    public List<Map<String, Object>> offeringOptions(Long counselorId, Long termId) {
        me(counselorId);
        Term term = termOf(termId);
        List<CourseOffering> list = offeringMapper.selectList(Wrappers.<CourseOffering>lambdaQuery()
                .eq(term != null, CourseOffering::getTermId, term == null ? null : term.getId())
                .orderByAsc(CourseOffering::getId));
        List<Map<String, Object>> out = new ArrayList<>();
        for (CourseOffering o : list) {
            Course c = courseMapper.selectById(o.getCourseId());
            TeachingClass tc = classMapper.selectOne(Wrappers.<TeachingClass>lambdaQuery()
                    .eq(TeachingClass::getOfferingId, o.getId()).last("limit 1"));
            Teacher t = tc == null ? null : teacherMapper.selectById(tc.getTeacherId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("offeringId", o.getId());
            m.put("courseId", o.getCourseId());
            m.put("courseName", c == null ? null : c.getCourseName());
            m.put("teacherName", t == null ? null : t.getTeacherName());
            m.put("className", tc == null ? null : tc.getClassName());
            out.add(m);
        }
        return out;
    }

    // ============================================================ 通知与回执
    public List<CounselorNoticeVO> notices(Long counselorId) {
        Counselor c = me(counselorId);
        List<Notification> list = notificationMapper.selectList(Wrappers.<Notification>lambdaQuery()
                .orderByDesc(Notification::getId).last("limit 200"));
        Map<Long, NotificationReceipt> reads = new LinkedHashMap<>();
        if (!list.isEmpty()) {
            for (NotificationReceipt r : receiptMapper.selectList(Wrappers.<NotificationReceipt>lambdaQuery()
                    .eq(NotificationReceipt::getRecipientId, c.getId())
                    .eq(NotificationReceipt::getRecipientRole, "COUNSELOR"))) {
                reads.put(r.getNotificationId(), r);
            }
        }
        List<CounselorNoticeVO> out = new ArrayList<>();
        for (Notification n : list) {
            CounselorNoticeVO v = new CounselorNoticeVO();
            v.setId(n.getId());
            v.setBizType(n.getBizType());
            v.setBizId(n.getBizId());
            v.setTitle(n.getTitle());
            v.setContent(n.getContent());
            v.setAudienceDesc(n.getAudienceDesc());
            v.setPriority(n.getPriority());
            v.setAggregateCount(n.getAggregateCount());
            v.setCreateTime(n.getCreateTime());
            NotificationReceipt r = reads.get(n.getId());
            v.setIsRead(r == null ? 0 : 1);
            v.setReadTime(r == null ? null : r.getReadTime());
            out.add(v);
        }
        return out;
    }

    @Transactional
    public void markRead(Long counselorId, Long notificationId) {
        Counselor c = me(counselorId);
        NotificationReceipt exist = receiptMapper.selectOne(Wrappers.<NotificationReceipt>lambdaQuery()
                .eq(NotificationReceipt::getNotificationId, notificationId)
                .eq(NotificationReceipt::getRecipientId, c.getId())
                .eq(NotificationReceipt::getRecipientRole, "COUNSELOR").last("limit 1"));
        if (exist != null) {
            return;
        }
        NotificationReceipt r = new NotificationReceipt();
        r.setNotificationId(notificationId);
        r.setRecipientId(c.getId());
        r.setRecipientRole("COUNSELOR");
        r.setIsRead(1);
        r.setReadTime(LocalDateTime.now());
        receiptMapper.insert(r);
    }
}
