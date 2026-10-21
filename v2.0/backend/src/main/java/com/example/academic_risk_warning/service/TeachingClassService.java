package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.EnrollRequest;
import com.example.academic_risk_warning.dto.TeachingClassSaveRequest;
import com.example.academic_risk_warning.entity.AdministrativeClass;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.CourseSession;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.entity.TeachingClassAdminClass;
import com.example.academic_risk_warning.mapper.AdministrativeClassMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.CourseSessionMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.mapper.TeachingClassAdminClassMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.vo.TeachingClassVO;
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
