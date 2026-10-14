package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.service.StudentQueryService;
import com.example.academic_risk_warning.service.TeacherService;
import com.example.academic_risk_warning.service.TokenService;
import com.example.academic_risk_warning.vo.StudentFullVO;
import com.example.academic_risk_warning.vo.TeacherCourseVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师接口：登录、查询所管班级学生学情
 */
@Validated
@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final TeacherService teacherService;
    private final StudentQueryService studentQueryService;
    private final TokenService tokenService;

    public TeacherController(TeacherService teacherService,
                             StudentQueryService studentQueryService,
                             TokenService tokenService) {
        this.teacherService = teacherService;
        this.studentQueryService = studentQueryService;
        this.tokenService = tokenService;
    }

    /**
     * 教师登录
     * POST /teacher/login
     */
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        try {
            Teacher teacher = teacherService.login(request.getTeacherNo(), request.getPassword());
            String token = tokenService.createToken(teacher.getId());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("data", teacher);
            result.put("token", token);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 刷新 Token（教师和学生通用）
     * POST /teacher/refresh
     */
    @PostMapping("/refresh")
    public Map<String, Object> refreshToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) auth = auth.substring(7).trim();
        String newToken = tokenService.refreshToken(auth);
        if (newToken == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "Token无效或已过期，请重新登录");
            return result;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("token", newToken);
        return result;
    }

    /**
     * 修改密码
     * POST /teacher/update-password
     */
    @PostMapping("/update-password")
    public Map<String, Object> updatePassword(@Valid @RequestBody PasswordRequest request, HttpServletRequest httpRequest) {
        Long teacherId = resolveTeacherId(httpRequest);
        if (teacherId == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "未登录，请携带 Token 或 X-Teacher-Id 请求头");
            return result;
        }
        try {
            teacherService.updatePassword(teacherId, request.getOldPassword(), request.getNewPassword());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "密码修改成功");
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 查询当前教师负责班级的全部学生学情
     * GET /teacher/my-students
     * 鉴权方式（二选一）：
     * 1. Header: Authorization: Bearer {登录返回的token}
     * 2. Header: X-Teacher-Id: {教师ID}
     */
    @GetMapping("/my-students")
    public Map<String, Object> myStudents(HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "未登录，请携带 Token 或 X-Teacher-Id 请求头");
            return result;
        }
        try {
            List<StudentFullVO> students = studentQueryService.listStudentsByTeacherId(teacherId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "查询成功");
            result.put("data", students);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 查询当前教师负责班级关联的课程列表（含学生数、预警数）
     * GET /teacher/my-courses
     */
    @GetMapping("/my-courses")
    public Map<String, Object> myCourses(HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "未登录，请携带 Token 或 X-Teacher-Id 请求头");
            return result;
        }
        try {
            List<TeacherCourseVO> courses = studentQueryService.listTeacherCourses(teacherId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "查询成功");
            result.put("data", courses);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 从 Token 或请求头解析 teacher_id
     */
    private Long resolveTeacherId(HttpServletRequest request) {
        String token = extractBearerToken(request.getHeader("Authorization"));
        Long teacherId = tokenService.validateAndGetTeacherId(token);
        if (teacherId != null) {
            return teacherId;
        }

        String headerTeacherId = request.getHeader("X-Teacher-Id");
        if (headerTeacherId != null && !headerTeacherId.isBlank()) {
            try {
                return Long.parseLong(headerTeacherId.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return authorization.trim();
    }

    /** 登录请求参数 */
    public static class LoginRequest {
        @NotBlank private String teacherNo;
        @NotBlank private String password;

        public String getTeacherNo() {
            return teacherNo;
        }

        public void setTeacherNo(String teacherNo) {
            this.teacherNo = teacherNo;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /** 修改密码请求参数 */
    public static class PasswordRequest {
        @NotBlank private String oldPassword;
        @NotBlank private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
