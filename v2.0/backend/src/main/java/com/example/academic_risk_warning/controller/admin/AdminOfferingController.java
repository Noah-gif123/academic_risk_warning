package com.example.academic_risk_warning.controller.admin;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.OfferingSaveRequest;
import com.example.academic_risk_warning.dto.TeachingClassSaveRequest;
import com.example.academic_risk_warning.dto.EnrollRequest;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.CourseSession;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.service.OfferingService;
import com.example.academic_risk_warning.service.TeachingClassService;
import com.example.academic_risk_warning.vo.OfferingVO;
import com.example.academic_risk_warning.vo.TeachingClassVO;
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
