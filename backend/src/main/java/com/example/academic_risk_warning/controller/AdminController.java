package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.common.PageResult;
import com.example.academic_risk_warning.entity.Admin;
import com.example.academic_risk_warning.entity.AlertRuleConfig;
import com.example.academic_risk_warning.entity.TeacherManagementLog;
import com.example.academic_risk_warning.service.*;
import com.example.academic_risk_warning.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员接口：登录、教师总览、教师详情、管理操作
 */
@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final TokenService tokenService;
    private final StudentQueryService studentQueryService;
    private final TeacherManagementService teacherManagementService;
    private final AlertQueryService alertQueryService;
    private final AlertConfigService alertConfigService;
    private final WarningGenerationService warningGenerationService;
    private final ExerciseService exerciseService;
    private final KnowledgeBaseBuilder knowledgeBaseBuilder;

    public AdminController(AdminService adminService,
                           TokenService tokenService,
                           StudentQueryService studentQueryService,
                           TeacherManagementService teacherManagementService,
                           AlertQueryService alertQueryService,
                           AlertConfigService alertConfigService,
                           WarningGenerationService warningGenerationService,
                           ExerciseService exerciseService,
                           KnowledgeBaseBuilder knowledgeBaseBuilder) {
        this.adminService = adminService;
        this.tokenService = tokenService;
        this.studentQueryService = studentQueryService;
        this.teacherManagementService = teacherManagementService;
        this.alertQueryService = alertQueryService;
        this.alertConfigService = alertConfigService;
        this.warningGenerationService = warningGenerationService;
        this.exerciseService = exerciseService;
        this.knowledgeBaseBuilder = knowledgeBaseBuilder;
    }

    // ==================== 登录 ====================

    /** POST /admin/login */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        try {
            Admin admin = adminService.login(request.getAdminNo(), request.getPassword());
            String token = tokenService.createAdminToken(admin.getId());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("data", admin);
            result.put("token", token);
            return result;
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    // ==================== 教师总览 ====================

    /** GET /admin/teachers — 获取所有教师列表（含风险统计） */
    @GetMapping("/teachers")
    public Map<String, Object> listTeachers(HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            List<AdminTeacherVO> teachers = adminService.listAllTeachers();
            return success("data", teachers);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /** GET /admin/teachers/{id}/courses — 获取指定教师的课程列表 */
    @GetMapping("/teachers/{teacherId}/courses")
    public Map<String, Object> getTeacherCourses(@PathVariable Long teacherId, HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            List<TeacherCourseVO> courses = studentQueryService.listTeacherCourses(teacherId);
            return success("data", courses);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /** GET /admin/teachers/{id}/students — 获取指定教师的学生学情列表 */
    @GetMapping("/teachers/{teacherId}/students")
    public Map<String, Object> getTeacherStudents(@PathVariable Long teacherId, HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            List<StudentFullVO> students = studentQueryService.listStudentsByTeacherId(teacherId);
            return success("data", students);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /** GET /admin/teachers/{id}/alerts — 获取指定教师的预警列表 */
    @GetMapping("/teachers/{teacherId}/alerts")
    public Map<String, Object> getTeacherAlerts(@PathVariable Long teacherId,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            PageResult<AlertRecordVO> result = alertQueryService.getAlertsByTeacherIdPaged(teacherId, status, null, page, size);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("success", true);
            r.put("message", "查询成功");
            r.put("data", result.getRecords());
            r.put("total", result.getTotal());
            r.put("page", page);
            r.put("size", size);
            return r;
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /** GET /admin/teachers/{id}/dashboard — 获取指定教师的仪表盘 */
    @GetMapping("/teachers/{teacherId}/dashboard")
    public Map<String, Object> getTeacherDashboard(@PathVariable Long teacherId, HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            Map<String, Object> dash = alertQueryService.getDashboard(teacherId, null);
            return success("data", dash);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    // ==================== 教师管理操作 ====================

    /** POST /admin/teachers/{id}/mark — 标记教师管理状态 */
    @PostMapping("/teachers/{teacherId}/mark")
    public Map<String, Object> markTeacher(@PathVariable Long teacherId,
                                            @Valid @RequestBody MarkRequest req,
                                            HttpServletRequest request) {
        Long adminId = resolveAdminId(request);
        if (adminId == null) return error("未登录");
        try {
            teacherManagementService.markStatus(adminId, teacherId, req.getStatus(), req.getRemark());
            return success("操作成功");
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /** POST /admin/teachers/{id}/notify — 向教师发送通知 */
    @PostMapping("/teachers/{teacherId}/notify")
    public Map<String, Object> notifyTeacher(@PathVariable Long teacherId,
                                              @Valid @RequestBody NotifyRequest req,
                                              HttpServletRequest request) {
        Long adminId = resolveAdminId(request);
        if (adminId == null) return error("未登录");
        try {
            teacherManagementService.sendNotice(adminId, teacherId, req.getTitle(), req.getContent());
            return success("通知已发送");
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /** POST /admin/teachers/{id}/remark — 添加管理备注 */
    @PostMapping("/teachers/{teacherId}/remark")
    public Map<String, Object> addRemark(@PathVariable Long teacherId,
                                          @Valid @RequestBody RemarkRequest req,
                                          HttpServletRequest request) {
        Long adminId = resolveAdminId(request);
        if (adminId == null) return error("未登录");
        try {
            teacherManagementService.addRemark(adminId, teacherId, req.getRemark());
            return success("备注已添加");
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /** GET /admin/teachers/{id}/logs — 获取指定教师管理日志 */
    @GetMapping("/teachers/{teacherId}/logs")
    public Map<String, Object> getTeacherLogs(@PathVariable Long teacherId, HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            List<TeacherManagementLog> logs = teacherManagementService.getLogs(teacherId);
            return success("data", logs);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /** GET /admin/logs — 获取全部管理操作日志 */
    @GetMapping("/logs")
    public Map<String, Object> getAllLogs(HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            List<TeacherManagementLog> logs = teacherManagementService.getAllLogs();
            return success("data", logs);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    // ==================== 预警规则配置 (管理员端CRUD) ====================

    /** GET /admin/config/list — 获取规则配置列表 */
    @GetMapping("/config/list")
    public Map<String, Object> getConfigList(@RequestParam(required = false) String systemType,
                                              HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            List<AlertRuleConfig> list = systemType != null
                    ? alertConfigService.listBySystem(systemType)
                    : alertConfigService.listAll();
            return success("data", list);
        } catch (Exception e) { return error(e.getMessage()); }
    }

    /** POST /admin/config — 创建配置 */
    @PostMapping("/config")
    public Map<String, Object> createConfig(@RequestBody AlertRuleConfig config,
                                             HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            alertConfigService.save(config);
            Map<String, Object> r = success("ok");
            r.put("data", config);
            return r;
        } catch (Exception e) { return error(e.getMessage()); }
    }

    /** PUT /admin/config/{id} — 更新配置 */
    @PutMapping("/config/{id}")
    public Map<String, Object> updateConfig(@PathVariable Long id, @RequestBody AlertRuleConfig config,
                                             HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            config.setId(id);
            alertConfigService.save(config);
            Map<String, Object> r = success("ok");
            r.put("data", config);
            return r;
        } catch (Exception e) { return error(e.getMessage()); }
    }

    /** DELETE /admin/config/{id} — 删除配置 */
    @DeleteMapping("/config/{id}")
    public Map<String, Object> deleteConfig(@PathVariable Long id, HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            alertConfigService.delete(id);
            return success("ok");
        } catch (Exception e) { return error(e.getMessage()); }
    }

    // ==================== 题库审核 ====================

    /** GET /admin/exercises/pending — 获取待审核练习题列表 */
    @GetMapping("/exercises/pending")
    public Map<String, Object> listPendingExercises(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size,
                                                     HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            return success("data", exerciseService.listPendingExercises(page, size));
        } catch (Exception e) { return error(e.getMessage()); }
    }

    /** POST /admin/exercises/{id}/approve — 审核通过 */
    @PostMapping("/exercises/{id}/approve")
    public Map<String, Object> approveExercise(@PathVariable Long id, HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            exerciseService.approveExercise(id);
            return success("审核通过");
        } catch (Exception e) { return error(e.getMessage()); }
    }

    /** POST /admin/exercises/{id}/reject — 审核驳回 */
    @PostMapping("/exercises/{id}/reject")
    public Map<String, Object> rejectExercise(@PathVariable Long id,
                                               @RequestBody Map<String, String> body,
                                               HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            exerciseService.rejectExercise(id, body.getOrDefault("remark", ""));
            return success("已驳回");
        } catch (Exception e) { return error(e.getMessage()); }
    }

    // ==================== 知识库构建 ====================

    /**
     * GET /api/admin/knowledge-base/build/{courseId}
     * 构建指定课程的完整知识库文本（知识点体系 + 题库索引）
     * 返回的 knowledgeText 可直接上传至百炼知识库应用
     */
    @GetMapping("/knowledge-base/build/{courseId}")
    public Map<String, Object> buildKnowledgeBase(@PathVariable Long courseId,
                                                   HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            String knowledgeText = knowledgeBaseBuilder.buildCourseKnowledgeText(courseId);
            Map<String, Object> r = success("knowledgeText", knowledgeText);
            r.put("courseId", courseId);
            r.put("length", knowledgeText.length());
            return r;
        } catch (Exception e) {
            return error("知识库构建失败: " + e.getMessage());
        }
    }

    /**
     * GET /api/admin/knowledge-base/preview/{courseId}
     * 仅预览知识点体系（不含题库），适合快速查看
     */
    @GetMapping("/knowledge-base/preview/{courseId}")
    public Map<String, Object> previewKnowledgeTree(@PathVariable Long courseId,
                                                     HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            String knowledgeTree = knowledgeBaseBuilder.buildKnowledgeTreeOnly(courseId);
            Map<String, Object> r = success("knowledgeTree", knowledgeTree);
            r.put("courseId", courseId);
            r.put("length", knowledgeTree.length());
            return r;
        } catch (Exception e) {
            return error("知识点预览失败: " + e.getMessage());
        }
    }

    /**
     * GET /api/admin/knowledge-base/build-all
     * 批量构建所有课程的知识库文本，返回 courseId→知识文本 的映射
     */
    @GetMapping("/knowledge-base/build-all")
    public Map<String, Object> buildAllKnowledgeBases(HttpServletRequest request) {
        if (resolveAdminId(request) == null) return error("未登录");
        try {
            Map<Long, String> allKnowledgeTexts = knowledgeBaseBuilder.buildAllCourses();
            Map<String, Object> r = success("allKnowledgeTexts", allKnowledgeTexts);
            r.put("courseCount", allKnowledgeTexts.size());
            return r;
        } catch (Exception e) {
            return error("批量构建失败: " + e.getMessage());
        }
    }

    // ==================== 鉴权辅助 ====================

    private Long resolveAdminId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) return null;
        if (auth.startsWith("Bearer ")) auth = auth.substring(7).trim();
        return tokenService.validateAndGetAdminId(auth);
    }

    // ==================== 响应辅助 ====================

    private Map<String, Object> success(String msg) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true); r.put("message", msg); return r;
    }

    private Map<String, Object> success(String key, Object data) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true); r.put("message", "查询成功");
        r.put(key, data); return r;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false); r.put("message", msg); return r;
    }

    // ==================== 请求体 ====================

    public static class LoginRequest {
        @NotBlank private String adminNo;
        @NotBlank private String password;
        public String getAdminNo() { return adminNo; }
        public void setAdminNo(String adminNo) { this.adminNo = adminNo; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class MarkRequest {
        @NotBlank private String status;
        private String remark;
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class NotifyRequest {
        @NotBlank private String title;
        @NotBlank private String content;
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class RemarkRequest {
        @NotBlank private String remark;
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
