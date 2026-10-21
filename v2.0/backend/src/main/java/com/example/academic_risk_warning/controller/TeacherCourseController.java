package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.entity.Term;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
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
