package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.StudentGoal;
import com.example.academic_risk_warning.service.StudentGoalService;
import com.example.academic_risk_warning.service.StudentLoginService;
import com.example.academic_risk_warning.service.StudentQueryService;
import com.example.academic_risk_warning.service.TokenService;
import com.example.academic_risk_warning.vo.StudentFullVO;
import com.example.academic_risk_warning.vo.StudentLoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 学生接口：登录、查询本人学情
 */
@Validated
@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentLoginService studentLoginService;
    private final StudentQueryService studentQueryService;
    private final TokenService tokenService;
    private final StudentGoalService studentGoalService;

    public StudentController(StudentLoginService studentLoginService,
                           StudentQueryService studentQueryService,
                           TokenService tokenService,
                           StudentGoalService studentGoalService) {
        this.studentLoginService = studentLoginService;
        this.studentQueryService = studentQueryService;
        this.tokenService = tokenService;
        this.studentGoalService = studentGoalService;
    }

    /**
     * 学生登录
     * POST /student/login
     */
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        try {
            StudentLoginVO vo = studentLoginService.login(request.getStudentNo(), request.getPassword());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("data", vo);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 查询当前登录学生的全套学情
     * GET /student/my-info
     * 鉴权方式：Header: Authorization: Bearer {登录返回的token}
     */
    @GetMapping("/my-info")
    public Map<String, Object> myInfo(HttpServletRequest request) {
        Long studentId = resolveStudentId(request);
        if (studentId == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "未登录，请携带 Token 请求头");
            return result;
        }

        Student student = studentLoginService.getStudentById(studentId);
        if (student == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "学生不存在");
            return result;
        }

        try {
            var students = studentQueryService.listStudentsByIds(java.util.List.of(studentId));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "查询成功");
            result.put("data", students.isEmpty() ? null : students.get(0));
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 修改密码
     * POST /student/update-password
     */
    @PostMapping("/update-password")
    public Map<String, Object> updatePassword(@Valid @RequestBody PasswordRequest request, HttpServletRequest httpRequest) {
        Long studentId = resolveStudentId(httpRequest);
        if (studentId == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "未登录，请携带 Token 请求头");
            return result;
        }
        try {
            studentLoginService.updatePassword(studentId, request.getOldPassword(), request.getNewPassword());
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
     * 刷新 Token（学生端）
     * POST /student/refresh
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

    // ==================== 学习目标 ====================

    /**
     * 查询我的学习目标
     * GET /student/goal?courseId=1
     */
    @GetMapping("/goal")
    public Map<String, Object> getGoal(Long courseId, HttpServletRequest request) {
        Long studentId = resolveStudentId(request);
        if (studentId == null) {
            return Map.of("success", false, "message", "未登录");
        }
        if (courseId == null) {
            return Map.of("success", false, "message", "请指定课程ID");
        }
        StudentGoal goal = studentGoalService.getByStudentAndCourse(studentId, courseId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", goal);
        return result;
    }

    /**
     * 设置学习目标
     * POST /student/goal
     */
    @PostMapping("/goal")
    public Map<String, Object> saveGoal(@Valid @RequestBody GoalRequest req, HttpServletRequest request) {
        Long studentId = resolveStudentId(request);
        if (studentId == null) {
            return Map.of("success", false, "message", "未登录");
        }
        try {
            StudentGoal goal = studentGoalService.saveOrUpdate(
                    studentId, req.getCourseId(), req.getGoalLevel(),
                    req.getTargetScore(), req.getMilestones(),
                    req.getNote(), req.getDeadline());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "目标设置成功");
            result.put("data", goal);
            return result;
        } catch (IllegalArgumentException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * 从 Token 解析 student_id（复用的是 TokenService teacherId 字段，实际存的是任意用户ID）
     */
    private Long resolveStudentId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) {
            return null;
        }
        if (auth.startsWith("Bearer ")) {
            auth = auth.substring(7).trim();
        }
        return tokenService.validateAndGetTeacherId(auth);
    }

    /** 登录请求参数 */
    public static class LoginRequest {
        @NotBlank private String studentNo;
        @NotBlank private String password;

        public String getStudentNo() {
            return studentNo;
        }

        public void setStudentNo(String studentNo) {
            this.studentNo = studentNo;
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

        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    /** 学习目标请求参数 */
    public static class GoalRequest {
        @NotNull private Long courseId;
        @NotBlank private String goalLevel;
        private Integer targetScore;
        private String milestones;
        private String note;
        private LocalDateTime deadline;

        public Long getCourseId() { return courseId; }
        public void setCourseId(Long v) { this.courseId = v; }
        public String getGoalLevel() { return goalLevel; }
        public void setGoalLevel(String v) { this.goalLevel = v; }
        public Integer getTargetScore() { return targetScore; }
        public void setTargetScore(Integer v) { this.targetScore = v; }
        public String getMilestones() { return milestones; }
        public void setMilestones(String v) { this.milestones = v; }
        public String getNote() { return note; }
        public void setNote(String v) { this.note = v; }
        public LocalDateTime getDeadline() { return deadline; }
        public void setDeadline(LocalDateTime v) { this.deadline = v; }
    }
}
