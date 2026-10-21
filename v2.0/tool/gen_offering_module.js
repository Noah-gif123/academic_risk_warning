/* gen_offering_module.js — 第一阶段 1-4 排课与教学班（开课 / 教学班 / 选课 / 课次生成 + 教师与学生端课程列表） */
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

// ---------------------------------------------------------------- dto
w('dto/OfferingSaveRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 新增 / 修改开课实例 */
@Data
public class OfferingSaveRequest {
    private Long id;
    @NotNull(message = "课程不能为空")
    private Long courseId;
    @NotNull(message = "学期不能为空")
    private Long termId;
    @NotNull(message = "计划周数不能为空")
    @Min(value = 1, message = "计划周数至少为 1")
    private Integer plannedWeeks;
    @NotNull(message = "每周课次不能为空")
    @Min(value = 1, message = "每周课次至少为 1")
    private Integer plannedTimesPerWeek;
    @NotNull(message = "及格线不能为空")
    private Integer passScore;
    private String remark;
}
`);

w('dto/TeachingClassSaveRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 新增 / 修改教学班 */
@Data
public class TeachingClassSaveRequest {
    private Long id;
    @NotNull(message = "开课实例不能为空")
    private Long offeringId;
    @NotNull(message = "任课教师不能为空")
    private Long teacherId;
    @NotBlank(message = "班级编码不能为空")
    private String classCode;
    private String className;
    private Integer capacity;
    private Integer isRetakeClass;
    /** 关联的行政班（用于一键把学生选进本教学班） */
    private List<Long> adminClassIds;
}
`);

w('dto/EnrollRequest.java', `
package ${P}.dto;

import lombok.Data;

import java.util.List;

/** 选课：按学生 ID 批量加入教学班 */
@Data
public class EnrollRequest {
    private List<Long> studentIds;
}
`);

// ---------------------------------------------------------------- vo
w('vo/OfferingVO.java', `
package ${P}.vo;

import lombok.Data;

/** 开课实例视图 */
@Data
public class OfferingVO {
    private Long id;
    private Long courseId;
    private String courseName;
    private String courseType;
    private Long termId;
    private String termName;
    private Integer plannedWeeks;
    private Integer plannedTimesPerWeek;
    private Integer plannedSessions;
    private Integer actualSessions;
    private Integer effectiveSessions;
    private String effectiveSource;
    private Integer passScore;
    private String status;
    private Integer classCount;
    private Integer enrollCount;
    private Integer sessionCount;
}
`);

w('vo/TeachingClassVO.java', `
package ${P}.vo;

import lombok.Data;

/** 教学班视图 */
@Data
public class TeachingClassVO {
    private Long id;
    private Long offeringId;
    private Long teacherId;
    private String teacherName;
    private String teacherNo;
    private String classCode;
    private String className;
    private Integer capacity;
    private Integer isRetakeClass;
    private String inviteCode;
    private Integer joinReviewEnabled;
    private String status;
    private Integer studentCount;
    private Integer sessionCount;
    private String adminClasses;
}
`);

w('vo/CourseBriefVO.java', `
package ${P}.vo;

import lombok.Data;

/** 学生端 / 教师端的课程卡片 */
@Data
public class CourseBriefVO {
    private Long offeringId;
    private String courseName;
    private String courseType;
    private Long teachingClassId;
    private String className;
    private String teacherName;
    private String termName;
    private Integer effectiveSessions;
    private String status;
    private Integer studentCount;
}
`);

// ---------------------------------------------------------------- service: 开课
w('service/OfferingService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.dto.OfferingSaveRequest;
import ${P}.entity.Course;
import ${P}.entity.CourseGroup;
import ${P}.entity.CourseOffering;
import ${P}.entity.CourseSession;
import ${P}.entity.Enrollment;
import ${P}.entity.TeachingClass;
import ${P}.entity.Term;
import ${P}.entity.TermCalendar;
import ${P}.mapper.CourseGroupMapper;
import ${P}.mapper.CourseMapper;
import ${P}.mapper.CourseOfferingMapper;
import ${P}.mapper.CourseSessionMapper;
import ${P}.mapper.EnrollmentMapper;
import ${P}.mapper.TeachingClassMapper;
import ${P}.mapper.TermCalendarMapper;
import ${P}.mapper.TermMapper;
import ${P}.vo.OfferingVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 开课实例：排课 + 课次生成 + 实际课次确认 */
@Service
@RequiredArgsConstructor
public class OfferingService {

    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final CourseGroupMapper groupMapper;
    private final TermMapper termMapper;
    private final TermCalendarMapper calendarMapper;
    private final TeachingClassMapper classMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CourseSessionMapper sessionMapper;
    private final AuditService auditService;

    public List<OfferingVO> list(Long termId) {
        List<CourseOffering> list = offeringMapper.selectList(Wrappers.<CourseOffering>lambdaQuery()
                .eq(termId != null, CourseOffering::getTermId, termId)
                .orderByDesc(CourseOffering::getId));
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Course> courses = courseMapper.selectBatchIds(
                        list.stream().map(CourseOffering::getCourseId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));
        Map<Long, Term> terms = termMapper.selectBatchIds(
                        list.stream().map(CourseOffering::getTermId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Term::getId, c -> c, (a, b) -> a));
        List<Long> offeringIds = list.stream().map(CourseOffering::getId).collect(Collectors.toList());
        Map<Long, Long> classCounts = new LinkedHashMap<>();
        Map<Long, Long> enrollCounts = new LinkedHashMap<>();
        Map<Long, Long> sessionCounts = new LinkedHashMap<>();
        classMapper.selectList(Wrappers.<TeachingClass>lambdaQuery().in(TeachingClass::getOfferingId, offeringIds))
                .forEach(c -> classCounts.merge(c.getOfferingId(), 1L, Long::sum));
        enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery().in(Enrollment::getOfferingId, offeringIds))
                .forEach(e -> enrollCounts.merge(e.getOfferingId(), 1L, Long::sum));
        sessionMapper.selectList(Wrappers.<CourseSession>lambdaQuery().in(CourseSession::getOfferingId, offeringIds))
                .forEach(s -> sessionCounts.merge(s.getOfferingId(), 1L, Long::sum));

        List<OfferingVO> out = new ArrayList<>();
        for (CourseOffering o : list) {
            OfferingVO vo = new OfferingVO();
            vo.setId(o.getId());
            vo.setCourseId(o.getCourseId());
            Course c = courses.get(o.getCourseId());
            if (c != null) {
                vo.setCourseName(c.getCourseName());
                vo.setCourseType(c.getCourseType());
            }
            vo.setTermId(o.getTermId());
            Term t = terms.get(o.getTermId());
            if (t != null) {
                vo.setTermName(t.getTermName());
            }
            vo.setPlannedWeeks(o.getPlannedWeeks());
            vo.setPlannedTimesPerWeek(o.getPlannedTimesPerWeek());
            vo.setPlannedSessions(o.getPlannedSessions());
            vo.setActualSessions(o.getActualSessions());
            vo.setEffectiveSessions(o.getEffectiveSessions());
            vo.setEffectiveSource(o.getEffectiveSource());
            vo.setPassScore(o.getPassScore());
            vo.setStatus(o.getStatus());
            vo.setClassCount(classCounts.getOrDefault(o.getId(), 0L).intValue());
            vo.setEnrollCount(enrollCounts.getOrDefault(o.getId(), 0L).intValue());
            vo.setSessionCount(sessionCounts.getOrDefault(o.getId(), 0L).intValue());
            out.add(vo);
        }
        return out;
    }

    @Transactional
    public CourseOffering save(OfferingSaveRequest req) {
        if (courseMapper.selectById(req.getCourseId()) == null) {
            throw new BizException("课程不存在");
        }
        if (termMapper.selectById(req.getTermId()) == null) {
            throw new BizException("学期不存在");
        }
        CourseOffering o;
        boolean create = req.getId() == null;
        if (create) {
            long dup = offeringMapper.selectCount(Wrappers.<CourseOffering>lambdaQuery()
                    .eq(CourseOffering::getCourseId, req.getCourseId())
                    .eq(CourseOffering::getTermId, req.getTermId()));
            if (dup > 0) {
                throw new BizException("该课程在该学期已存在开课实例");
            }
            o = new CourseOffering();
        } else {
            o = offeringMapper.selectById(req.getId());
            if (o == null) {
                throw new BizException("开课实例不存在");
            }
        }
        o.setCourseId(req.getCourseId());
        o.setTermId(req.getTermId());
        o.setPlannedWeeks(req.getPlannedWeeks());
        o.setPlannedTimesPerWeek(req.getPlannedTimesPerWeek());
        o.setPlannedSessions(req.getPlannedWeeks() * req.getPlannedTimesPerWeek());
        o.setPassScore(req.getPassScore());
        o.setRemark(req.getRemark());
        if (create) {
            CourseGroup g = groupMapper.selectOne(Wrappers.<CourseGroup>lambdaQuery()
                    .eq(CourseGroup::getCourseId, req.getCourseId())
                    .eq(CourseGroup::getTermId, req.getTermId()).last("limit 1"));
            if (g == null) {
                g = new CourseGroup();
                g.setCourseId(req.getCourseId());
                g.setTermId(req.getTermId());
                g.setLevel("L2");
                g.setLevelRequestStatus("NONE");
                g.setStatus("ACTIVE");
                groupMapper.insert(g);
            }
            o.setGroupId(g.getId());
            o.setStatus("ACTIVE");
            offeringMapper.insert(o);
        } else {
            offeringMapper.updateById(o);
        }
        auditService.record("COURSE_OFFERING", o.getId(), create ? "CREATE" : "UPDATE", null, null,
                (create ? "新增开课 " : "修改开课 ") + "计划课次 " + o.getPlannedSessions());
        return o;
    }

    /** 教务确认实际课次：effective_sessions 用于缺课门槛与覆盖率的分母 */
    @Transactional
    public CourseOffering confirmActual(Long offeringId) {
        CourseOffering o = offeringMapper.selectById(offeringId);
        if (o == null) {
            throw new BizException("开课实例不存在");
        }
        // 实际课次优先取“已生成的课次数”（已发生/已排定的课）；没有再退回计划课次
        long generated = sessionMapper.selectCount(Wrappers.<CourseSession>lambdaQuery()
                .eq(CourseSession::getOfferingId, offeringId)
                .eq(CourseSession::getIsVoid, 0));
        int actual;
        String source;
        if (o.getActualSessions() != null) {
            actual = o.getActualSessions();
            source = "ACTUAL";
        } else if (generated > 0) {
            actual = (int) generated;
            source = "SESSIONS";
        } else {
            actual = o.getPlannedSessions() == null ? 0 : o.getPlannedSessions();
            source = "PLANNED";
        }
        o.setActualSessions(actual);
        o.setEffectiveSessions(actual);
        o.setEffectiveSource(source);
        o.setActualSubmitStatus("CONFIRMED");
        o.setActualConfirmBy(1L);
        o.setActualConfirmTime(LocalDateTime.now());
        offeringMapper.updateById(o);
        auditService.record("COURSE_OFFERING", offeringId, "CONFIRM_ACTUAL", null, null,
                "确认有效课次 " + actual + "（口径：有效课次作为 1/3 门槛与覆盖率分母）");
        return o;
    }

    /** 按周历生成课次（跳过假期周与考试周），一次课一行 */
    @Transactional
    public int generateSessions(Long offeringId, Long teachingClassId) {
        CourseOffering o = offeringMapper.selectById(offeringId);
        if (o == null) {
            throw new BizException("开课实例不存在");
        }
        TeachingClass tc = classMapper.selectById(teachingClassId);
        if (tc == null || !offeringId.equals(tc.getOfferingId())) {
            throw new BizException("教学班不存在或不属于该开课");
        }
        long exists = sessionMapper.selectCount(Wrappers.<CourseSession>lambdaQuery()
                .eq(CourseSession::getTeachingClassId, teachingClassId));
        if (exists > 0) {
            throw new BizException("该教学班已生成课次，如需重建请先在数据库清理（本版不提供一键重建）");
        }
        List<TermCalendar> weeks = calendarMapper.selectList(Wrappers.<TermCalendar>lambdaQuery()
                .eq(TermCalendar::getTermId, o.getTermId()).orderByAsc(TermCalendar::getWeekNo));
        if (weeks.isEmpty()) {
            throw new BizException("该学期还没有周历，请先在「基础数据 → 学期与教学周历」生成周历");
        }
        int times = o.getPlannedTimesPerWeek() == null ? 1 : o.getPlannedTimesPerWeek();
        int sessionNo = 0;
        for (TermCalendar wk : weeks) {
            if ("HOLIDAY".equals(wk.getWeekType()) || "EXAM".equals(wk.getWeekType())) {
                continue;
            }
            for (int i = 0; i < times; i++) {
                sessionNo++;
                CourseSession s = new CourseSession();
                s.setOfferingId(offeringId);
                s.setTeachingClassId(teachingClassId);
                s.setTermId(o.getTermId());
                s.setWeekNo(wk.getWeekNo());
                s.setSessionNo(sessionNo);
                s.setSessionDate(wk.getStartDate().plusDays(Math.min(i, 6)));
                s.setSessionType("NORMAL");
                s.setStatus("NORMAL");
                s.setRollCallStatus("NOT_CALLED");
                s.setIsVoid(0);
                sessionMapper.insert(s);
            }
        }
        auditService.record("COURSE_SESSION", offeringId, "GENERATE", null, null,
                "为教学班 " + tc.getClassCode() + " 生成课次 " + sessionNo + " 次");
        return sessionNo;
    }

    public List<CourseSession> listSessions(Long offeringId, Long teachingClassId) {
        return sessionMapper.selectList(Wrappers.<CourseSession>lambdaQuery()
                .eq(offeringId != null, CourseSession::getOfferingId, offeringId)
                .eq(teachingClassId != null, CourseSession::getTeachingClassId, teachingClassId)
                .orderByAsc(CourseSession::getSessionNo));
    }
}
`);

// ---------------------------------------------------------------- service: 教学班与选课
w('service/TeachingClassService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.dto.EnrollRequest;
import ${P}.dto.TeachingClassSaveRequest;
import ${P}.entity.AdministrativeClass;
import ${P}.entity.CourseOffering;
import ${P}.entity.CourseSession;
import ${P}.entity.Enrollment;
import ${P}.entity.Student;
import ${P}.entity.Teacher;
import ${P}.entity.TeachingClass;
import ${P}.entity.TeachingClassAdminClass;
import ${P}.mapper.AdministrativeClassMapper;
import ${P}.mapper.CourseOfferingMapper;
import ${P}.mapper.CourseSessionMapper;
import ${P}.mapper.EnrollmentMapper;
import ${P}.mapper.StudentMapper;
import ${P}.mapper.TeacherMapper;
import ${P}.mapper.TeachingClassAdminClassMapper;
import ${P}.mapper.TeachingClassMapper;
import ${P}.vo.TeachingClassVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 教学班（1 位教师）与选课 */
@Service
@RequiredArgsConstructor
public class TeachingClassService {

    private final TeachingClassMapper classMapper;
    private final TeachingClassAdminClassMapper tcAcMapper;
    private final AdministrativeClassMapper adminClassMapper;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseSessionMapper sessionMapper;
    private final AuditService auditService;

    public List<TeachingClassVO> list(Long offeringId) {
        List<TeachingClass> list = classMapper.selectList(Wrappers.<TeachingClass>lambdaQuery()
                .eq(offeringId != null, TeachingClass::getOfferingId, offeringId)
                .orderByAsc(TeachingClass::getClassCode));
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Teacher> teachers = teacherMapper.selectBatchIds(
                        list.stream().map(TeachingClass::getTeacherId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Teacher::getId, t -> t, (a, b) -> a));
        List<Long> classIds = list.stream().map(TeachingClass::getId).collect(Collectors.toList());
        Map<Long, Long> studentCounts = new LinkedHashMap<>();
        enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                        .in(Enrollment::getTeachingClassId, classIds))
                .forEach(e -> studentCounts.merge(e.getTeachingClassId(), 1L, Long::sum));
        Map<Long, Long> sessionCounts = new LinkedHashMap<>();
        sessionMapper.selectList(Wrappers.<CourseSession>lambdaQuery()
                        .in(CourseSession::getTeachingClassId, classIds))
                .forEach(s -> sessionCounts.merge(s.getTeachingClassId(), 1L, Long::sum));
        Map<Long, List<Long>> bindings = new LinkedHashMap<>();
        tcAcMapper.selectList(Wrappers.<TeachingClassAdminClass>lambdaQuery()
                        .in(TeachingClassAdminClass::getTeachingClassId, classIds))
                .forEach(b -> bindings.computeIfAbsent(b.getTeachingClassId(), k -> new ArrayList<>()).add(b.getAdminClassId()));
        Map<Long, String> adminClassNames = adminClassMapper.selectList(null).stream()
                .collect(Collectors.toMap(AdministrativeClass::getId, AdministrativeClass::getClassName, (a, b) -> a));

        List<TeachingClassVO> out = new ArrayList<>();
        for (TeachingClass c : list) {
            TeachingClassVO vo = new TeachingClassVO();
            vo.setId(c.getId());
            vo.setOfferingId(c.getOfferingId());
            vo.setTeacherId(c.getTeacherId());
            Teacher t = teachers.get(c.getTeacherId());
            if (t != null) {
                vo.setTeacherName(t.getTeacherName());
                vo.setTeacherNo(t.getTeacherNo());
            }
            vo.setClassCode(c.getClassCode());
            vo.setClassName(c.getClassName());
            vo.setCapacity(c.getCapacity());
            vo.setIsRetakeClass(c.getIsRetakeClass());
            vo.setInviteCode(c.getInviteCode());
            vo.setJoinReviewEnabled(c.getJoinReviewEnabled());
            vo.setStatus(c.getStatus());
            vo.setStudentCount(studentCounts.getOrDefault(c.getId(), 0L).intValue());
            vo.setSessionCount(sessionCounts.getOrDefault(c.getId(), 0L).intValue());
            List<Long> acIds = bindings.getOrDefault(c.getId(), new ArrayList<>());
            vo.setAdminClasses(acIds.stream().map(adminClassNames::get).filter(n -> n != null)
                    .collect(Collectors.joining("、")));
            out.add(vo);
        }
        return out;
    }

    @Transactional
    public TeachingClass save(TeachingClassSaveRequest req) {
        if (offeringMapper.selectById(req.getOfferingId()) == null) {
            throw new BizException("开课实例不存在");
        }
        if (teacherMapper.selectById(req.getTeacherId()) == null) {
            throw new BizException("教师不存在");
        }
        TeachingClass c;
        boolean create = req.getId() == null;
        if (create) {
            long dup = classMapper.selectCount(Wrappers.<TeachingClass>lambdaQuery()
                    .eq(TeachingClass::getOfferingId, req.getOfferingId())
                    .eq(TeachingClass::getClassCode, req.getClassCode()));
            if (dup > 0) {
                throw new BizException("该开课下班级编码已存在：" + req.getClassCode());
            }
            c = new TeachingClass();
        } else {
            c = classMapper.selectById(req.getId());
            if (c == null) {
                throw new BizException("教学班不存在");
            }
        }
        c.setOfferingId(req.getOfferingId());
        c.setTeacherId(req.getTeacherId());
        c.setClassCode(req.getClassCode());
        c.setClassName(req.getClassName());
        c.setCapacity(req.getCapacity());
        c.setIsRetakeClass(req.getIsRetakeClass() == null ? 0 : req.getIsRetakeClass());
        if (create) {
            c.setStatus("ACTIVE");
            c.setJoinReviewEnabled(0);
            classMapper.insert(c);
        } else {
            classMapper.updateById(c);
        }
        if (req.getAdminClassIds() != null) {
            tcAcMapper.delete(Wrappers.<TeachingClassAdminClass>lambdaQuery()
                    .eq(TeachingClassAdminClass::getTeachingClassId, c.getId()));
            for (Long acId : req.getAdminClassIds()) {
                TeachingClassAdminClass link = new TeachingClassAdminClass();
                link.setTeachingClassId(c.getId());
                link.setAdminClassId(acId);
                tcAcMapper.insert(link);
            }
        }
        auditService.record("TEACHING_CLASS", c.getId(), create ? "CREATE" : "UPDATE", null, null,
                (create ? "新增教学班 " : "修改教学班 ") + c.getClassCode());
        return c;
    }

    /** 把教学班关联的行政班学生批量选课（幂等，已选过的跳过） */
    @Transactional
    public int enrollBoundAdminClasses(Long classId) {
        TeachingClass c = classMapper.selectById(classId);
        if (c == null) {
            throw new BizException("教学班不存在");
        }
        List<Long> acIds = tcAcMapper.selectList(Wrappers.<TeachingClassAdminClass>lambdaQuery()
                        .eq(TeachingClassAdminClass::getTeachingClassId, classId))
                .stream().map(TeachingClassAdminClass::getAdminClassId).collect(Collectors.toList());
        if (acIds.isEmpty()) {
            throw new BizException("该教学班还没有关联行政班");
        }
        List<Student> students = studentMapper.selectList(Wrappers.<Student>lambdaQuery()
                .in(Student::getAdminClassId, acIds)
                .eq(Student::getStatus, "IN_SCHOOL"));
        int created = 0;
        for (Student s : students) {
            created += enrollOne(c, s.getId());
        }
        auditService.record("ENROLLMENT", classId, "ENROLL_BY_CLASS", null, null,
                "按行政班选课：教学班 " + c.getClassCode() + " 新增 " + created + " 人");
        return created;
    }

    /** 手工按学生选课 */
    @Transactional
    public int enrollStudents(Long classId, EnrollRequest req) {
        TeachingClass c = classMapper.selectById(classId);
        if (c == null) {
            throw new BizException("教学班不存在");
        }
        if (req.getStudentIds() == null || req.getStudentIds().isEmpty()) {
            throw new BizException("请至少选择一名学生");
        }
        int created = 0;
        for (Long sid : req.getStudentIds()) {
            created += enrollOne(c, sid);
        }
        auditService.record("ENROLLMENT", classId, "ENROLL_BY_STUDENT", null, null,
                "手工选课：教学班 " + c.getClassCode() + " 新增 " + created + " 人");
        return created;
    }

    private int enrollOne(TeachingClass c, Long studentId) {
        long exists = enrollmentMapper.selectCount(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, c.getId())
                .eq(Enrollment::getStudentId, studentId));
        if (exists > 0) {
            return 0;
        }
        Enrollment e = new Enrollment();
        e.setStudentId(studentId);
        e.setTeachingClassId(c.getId());
        e.setOfferingId(c.getOfferingId());
        CourseOffering o = offeringMapper.selectById(c.getOfferingId());
        e.setTermId(o == null ? null : o.getTermId());
        e.setStatus("ENROLLED");
        e.setRetakeTimes(0);
        e.setEnrollTime(LocalDateTime.now());
        enrollmentMapper.insert(e);
        return 1;
    }

    /** 教学班学生名单 */
    public List<Map<String, Object>> listStudents(Long classId) {
        List<Enrollment> enrollments = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, classId).orderByAsc(Enrollment::getStudentId));
        if (enrollments.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Student> students = studentMapper.selectBatchIds(
                        enrollments.stream().map(Enrollment::getStudentId).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Student::getId, s -> s, (a, b) -> a));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Enrollment e : enrollments) {
            Student s = students.get(e.getStudentId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("enrollmentId", e.getId());
            m.put("studentId", e.getStudentId());
            m.put("studentNo", s == null ? "" : s.getStudentNo());
            m.put("studentName", s == null ? "" : s.getStudentName());
            m.put("status", e.getStatus());
            out.add(m);
        }
        return out;
    }
}
`);

// ---------------------------------------------------------------- controller: 教务端排课
w('controller/admin/AdminOfferingController.java', `
package ${P}.controller.admin;

import ${P}.common.Result;
import ${P}.dto.OfferingSaveRequest;
import ${P}.dto.TeachingClassSaveRequest;
import ${P}.dto.EnrollRequest;
import ${P}.entity.Course;
import ${P}.entity.CourseOffering;
import ${P}.entity.CourseSession;
import ${P}.entity.Teacher;
import ${P}.entity.TeachingClass;
import ${P}.mapper.CourseMapper;
import ${P}.mapper.TeacherMapper;
import ${P}.security.RequireRole;
import ${P}.service.OfferingService;
import ${P}.service.TeachingClassService;
import ${P}.vo.OfferingVO;
import ${P}.vo.TeachingClassVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 教务端 · 排课与教学班 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminOfferingController {

    private final OfferingService offeringService;
    private final TeachingClassService classService;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;

    @GetMapping("/courses")
    public Result<List<Course>> courses() {
        return Result.ok(courseMapper.selectList(Wrappers.<Course>lambdaQuery().orderByAsc(Course::getId)));
    }

    @GetMapping("/teachers")
    public Result<List<Teacher>> teachers() {
        return Result.ok(teacherMapper.selectList(Wrappers.<Teacher>lambdaQuery()
                .eq(Teacher::getStatus, "ACTIVE").orderByAsc(Teacher::getTeacherNo)));
    }

    @GetMapping("/offerings")
    public Result<List<OfferingVO>> offerings(@RequestParam(required = false) Long termId) {
        return Result.ok(offeringService.list(termId));
    }

    @PostMapping("/offerings")
    public Result<CourseOffering> saveOffering(@Valid @RequestBody OfferingSaveRequest request) {
        return Result.ok(offeringService.save(request));
    }

    @PostMapping("/offerings/{id}/confirm-actual")
    public Result<CourseOffering> confirmActual(@PathVariable Long id) {
        return Result.ok(offeringService.confirmActual(id));
    }

    @PostMapping("/offerings/{id}/sessions/generate")
    public Result<Integer> generateSessions(@PathVariable Long id, @RequestParam Long teachingClassId) {
        return Result.ok(offeringService.generateSessions(id, teachingClassId));
    }

    @GetMapping("/offerings/{id}/sessions")
    public Result<List<CourseSession>> sessions(@PathVariable Long id,
                                                @RequestParam(required = false) Long teachingClassId) {
        return Result.ok(offeringService.listSessions(id, teachingClassId));
    }

    @GetMapping("/teaching-classes")
    public Result<List<TeachingClassVO>> classes(@RequestParam(required = false) Long offeringId) {
        return Result.ok(classService.list(offeringId));
    }

    @PostMapping("/teaching-classes")
    public Result<TeachingClass> saveClass(@Valid @RequestBody TeachingClassSaveRequest request) {
        return Result.ok(classService.save(request));
    }

    @PostMapping("/teaching-classes/{id}/enroll/admin-classes")
    public Result<Integer> enrollByClass(@PathVariable Long id) {
        return Result.ok(classService.enrollBoundAdminClasses(id));
    }

    @PostMapping("/teaching-classes/{id}/enroll/students")
    public Result<Integer> enrollByStudents(@PathVariable Long id, @RequestBody EnrollRequest request) {
        return Result.ok(classService.enrollStudents(id, request));
    }

    @GetMapping("/teaching-classes/{id}/students")
    public Result<List<Map<String, Object>>> students(@PathVariable Long id) {
        return Result.ok(classService.listStudents(id));
    }
}
`);

// ---------------------------------------------------------------- controller: 教师与学生端「我的课程」
w('controller/TeacherCourseController.java', `
package ${P}.controller;

import ${P}.common.BizException;
import ${P}.common.Result;
import ${P}.entity.Course;
import ${P}.entity.CourseOffering;
import ${P}.entity.Enrollment;
import ${P}.entity.Teacher;
import ${P}.entity.TeachingClass;
import ${P}.entity.Term;
import ${P}.mapper.CourseMapper;
import ${P}.mapper.CourseOfferingMapper;
import ${P}.mapper.EnrollmentMapper;
import ${P}.mapper.TeacherMapper;
import ${P}.mapper.TeachingClassMapper;
import ${P}.mapper.TermMapper;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.vo.CourseBriefVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 教师端 · 我的教学课程 */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherCourseController {

    private final TeachingClassMapper classMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final TermMapper termMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final TeacherMapper teacherMapper;

    @GetMapping("/offerings")
    public Result<List<CourseBriefVO>> myOfferings() {
        LoginUser user = UserContext.require();
        Teacher teacher = teacherMapper.selectById(user.getRefId());
        if (teacher == null) {
            throw new BizException("教师档案不存在");
        }
        List<TeachingClass> classes = classMapper.selectList(Wrappers.<TeachingClass>lambdaQuery()
                .eq(TeachingClass::getTeacherId, teacher.getId()).orderByAsc(TeachingClass::getId));
        if (classes.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }
        Map<Long, CourseOffering> offerings = offeringMapper.selectBatchIds(
                        classes.stream().map(TeachingClass::getOfferingId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(CourseOffering::getId, o -> o, (a, b) -> a));
        Map<Long, Course> courses = courseMapper.selectBatchIds(
                        offerings.values().stream().map(CourseOffering::getCourseId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));
        Map<Long, Term> terms = termMapper.selectBatchIds(
                        offerings.values().stream().map(CourseOffering::getTermId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Term::getId, t -> t, (a, b) -> a));
        List<CourseBriefVO> out = new ArrayList<>();
        for (TeachingClass c : classes) {
            CourseOffering o = offerings.get(c.getOfferingId());
            if (o == null) {
                continue;
            }
            Course course = courses.get(o.getCourseId());
            Term term = terms.get(o.getTermId());
            CourseBriefVO vo = new CourseBriefVO();
            vo.setOfferingId(o.getId());
            vo.setTeachingClassId(c.getId());
            vo.setClassName(c.getClassName());
            vo.setCourseName(course == null ? "" : course.getCourseName());
            vo.setCourseType(course == null ? "" : course.getCourseType());
            vo.setTeacherName(teacher.getTeacherName());
            vo.setTermName(term == null ? "" : term.getTermName());
            vo.setEffectiveSessions(o.getEffectiveSessions());
            vo.setStatus(c.getStatus());
            vo.setStudentCount(Long.valueOf(enrollmentMapper.selectCount(Wrappers.<Enrollment>lambdaQuery()
                    .eq(Enrollment::getTeachingClassId, c.getId()))).intValue());
            out.add(vo);
        }
        return Result.ok(out);
    }
}
`);

w('controller/StudentCourseController.java', `
package ${P}.controller;

import ${P}.common.BizException;
import ${P}.common.Result;
import ${P}.entity.Course;
import ${P}.entity.CourseOffering;
import ${P}.entity.Enrollment;
import ${P}.entity.Student;
import ${P}.entity.Teacher;
import ${P}.entity.TeachingClass;
import ${P}.entity.Term;
import ${P}.mapper.CourseMapper;
import ${P}.mapper.CourseOfferingMapper;
import ${P}.mapper.EnrollmentMapper;
import ${P}.mapper.StudentMapper;
import ${P}.mapper.TeacherMapper;
import ${P}.mapper.TeachingClassMapper;
import ${P}.mapper.TermMapper;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.vo.CourseBriefVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** 学生端 · 我的课程 */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentCourseController {

    private final StudentMapper studentMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final TeachingClassMapper classMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final TermMapper termMapper;
    private final TeacherMapper teacherMapper;

    @GetMapping("/courses")
    public Result<List<CourseBriefVO>> myCourses() {
        LoginUser user = UserContext.require();
        Student student = studentMapper.selectById(user.getRefId());
        if (student == null) {
            throw new BizException("学生档案不存在");
        }
        List<Enrollment> enrollments = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getStudentId, student.getId())
                .eq(Enrollment::getStatus, "ENROLLED")
                .orderByAsc(Enrollment::getId));
        if (enrollments.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }
        java.util.Map<Long, TeachingClass> classes = classMapper.selectBatchIds(
                        enrollments.stream().map(Enrollment::getTeachingClassId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(TeachingClass::getId, c -> c, (a, b) -> a));
        java.util.Map<Long, CourseOffering> offerings = offeringMapper.selectBatchIds(
                        enrollments.stream().map(Enrollment::getOfferingId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(CourseOffering::getId, o -> o, (a, b) -> a));
        java.util.Map<Long, Course> courses = courseMapper.selectBatchIds(
                        offerings.values().stream().map(CourseOffering::getCourseId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));
        java.util.Map<Long, Term> terms = termMapper.selectBatchIds(
                        offerings.values().stream().map(CourseOffering::getTermId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Term::getId, t -> t, (a, b) -> a));
        java.util.Map<Long, Teacher> teachers = teacherMapper.selectBatchIds(
                        classes.values().stream().map(TeachingClass::getTeacherId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Teacher::getId, t -> t, (a, b) -> a));

        List<CourseBriefVO> out = new ArrayList<>();
        for (Enrollment e : enrollments) {
            TeachingClass tc = classes.get(e.getTeachingClassId());
            CourseOffering o = offerings.get(e.getOfferingId());
            if (tc == null || o == null) {
                continue;
            }
            Course course = courses.get(o.getCourseId());
            Term term = terms.get(o.getTermId());
            Teacher teacher = teachers.get(tc.getTeacherId());
            CourseBriefVO vo = new CourseBriefVO();
            vo.setOfferingId(o.getId());
            vo.setTeachingClassId(tc.getId());
            vo.setClassName(tc.getClassName());
            vo.setCourseName(course == null ? "" : course.getCourseName());
            vo.setCourseType(course == null ? "" : course.getCourseType());
            vo.setTeacherName(teacher == null ? "" : teacher.getTeacherName());
            vo.setTermName(term == null ? "" : term.getTermName());
            vo.setEffectiveSessions(o.getEffectiveSessions());
            vo.setStatus(tc.getStatus());
            out.add(vo);
        }
        return Result.ok(out);
    }
}
`);

console.log('排课与教学班模块：Java 代码生成完成');
