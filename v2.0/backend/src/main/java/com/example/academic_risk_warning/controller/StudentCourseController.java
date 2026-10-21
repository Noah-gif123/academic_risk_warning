package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.entity.Term;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.mapper.TermMapper;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.vo.CourseBriefVO;
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
