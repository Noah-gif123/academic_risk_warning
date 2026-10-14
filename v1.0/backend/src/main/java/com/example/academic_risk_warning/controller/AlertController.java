package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.common.PageResult;
import com.example.academic_risk_warning.entity.AlertOperationLog;
import com.example.academic_risk_warning.entity.InterventionRecord;
import com.example.academic_risk_warning.entity.Notification;
import com.example.academic_risk_warning.service.AlertQueryService;
import com.example.academic_risk_warning.service.AlertSummaryService;
import com.example.academic_risk_warning.service.InterventionService;
import com.example.academic_risk_warning.service.NotificationService;
import com.example.academic_risk_warning.service.TokenService;
import com.example.academic_risk_warning.service.WarningGenerationService;
import com.example.academic_risk_warning.vo.AlertRecordVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 预警管理接口 v2
 * 新增: 预警详情、干预管理、效果回测、闭环关闭、重新打开、操作日志查询
 */
@Validated
@RestController
@RequestMapping("/api/alert")
public class AlertController {

    private final AlertQueryService alertQueryService;
    private final WarningGenerationService warningGenerationService;
    private final TokenService tokenService;
    private final InterventionService interventionService;
    private final NotificationService notificationService;
    private final AlertSummaryService alertSummaryService;

    public AlertController(AlertQueryService alertQueryService,
                           WarningGenerationService warningGenerationService,
                           TokenService tokenService,
                           InterventionService interventionService,
                           NotificationService notificationService,
                           AlertSummaryService alertSummaryService) {
        this.alertQueryService = alertQueryService;
        this.warningGenerationService = warningGenerationService;
        this.tokenService = tokenService;
        this.interventionService = interventionService;
        this.notificationService = notificationService;
        this.alertSummaryService = alertSummaryService;
    }

    // ==================== 教师端 ====================

    /** GET /alert/teacher/list */
    @GetMapping("/teacher/list")
    public Map<String, Object> getTeacherAlerts(@RequestParam(required = false) String status,
                                                 @RequestParam(required = false) Long courseId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            PageResult<AlertRecordVO> result = alertQueryService.getAlertsByTeacherIdPaged(teacherId, status, courseId, page, size);
            return successPage(result.getRecords(), (int) result.getTotal());
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** GET /alert/teacher/stats */
    @GetMapping("/teacher/stats")
    public Map<String, Object> getTeacherStats(@RequestParam(required = false) Long courseId,
                                                HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            Map<String, Object> stats = alertQueryService.getAlertStatistics(teacherId, courseId);
            Map<String, Object> result = successResult();
            result.put("data", stats);
            return result;
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** GET /alert/teacher/dashboard (可视化仪表盘) */
    @GetMapping("/teacher/dashboard")
    public Map<String, Object> getDashboard(@RequestParam(required = false) Long courseId,
                                             HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            Map<String, Object> dash = alertQueryService.getDashboard(teacherId, courseId);
            Map<String, Object> result = successResult();
            result.put("data", dash);
            return result;
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** GET /alert/teacher/top-risk (风险排行) */
    @GetMapping("/teacher/top-risk")
    public Map<String, Object> getTopRisk(@RequestParam(defaultValue = "10") @Max(100) int limit,
                                           @RequestParam(required = false) Long courseId,
                                           HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            return successPage(alertQueryService.getTopRiskStudents(teacherId, limit, courseId), 0);
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** GET /alert/teacher/summaries (每日/每周汇总列表) */
    @GetMapping("/teacher/summaries")
    public Map<String, Object> getSummaries(@RequestParam(defaultValue = "DAILY") String period,
                                             @RequestParam(defaultValue = "10") int limit,
                                             HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            Map<String, Object> result = successResult();
            result.put("data", alertSummaryService.getSummaries(teacherId, period, limit));
            return result;
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** GET /alert/teacher/class-heatmap (班级热力图) */
    @GetMapping("/teacher/class-heatmap")
    public Map<String, Object> getClassHeatmap(HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            Map<String, Object> result = successResult();
            result.put("data", alertQueryService.getClassHeatmap(teacherId));
            return result;
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** GET /alert/student/{id}/radar (学生风险雷达图；教师可查任意学生，学生仅可查本人) */
    @GetMapping("/student/{studentId}/radar")
    public Map<String, Object> getStudentRadar(@PathVariable Long studentId,
                                                @RequestParam Long courseId,
                                                HttpServletRequest request) {
        Long loginTeacherId = resolveTeacherId(request);
        Long loginStudentId = resolveStudentId(request);
        // 教师可查任意学生；学生只能查自己的画像
        if (loginTeacherId == null && !studentId.equals(loginStudentId))
            return errorResult("未登录或无权查看");
        try {
            Map<String, Object> result = successResult();
            result.put("data", alertQueryService.getStudentRadar(studentId, courseId));
            return result;
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** GET /alert/student/{id}/trend (学生风险趋势) */
    @GetMapping("/student/{studentId}/trend")
    public Map<String, Object> getStudentTrend(@PathVariable Long studentId,
                                                @RequestParam Long courseId,
                                                HttpServletRequest request) {
        if (resolveTeacherId(request) == null && resolveStudentId(request) == null)
            return errorResult("未登录");
        try {
            Map<String, Object> result = successResult();
            result.put("data", alertQueryService.getStudentRiskTrend(studentId, courseId));
            return result;
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** POST /alert/teacher/generate */
    @PostMapping("/teacher/generate")
    public Map<String, Object> generateWarnings(HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            int count = warningGenerationService.generateWarningsForTeacher(teacherId);
            return successResult("成功生成 " + count + " 条预警", Map.of("count", count));
        } catch (Exception e) {
            return errorResult("生成预警失败：" + e.getMessage());
        }
    }

    /** POST /alert/teacher/generate-summary (手动生成汇总报告) */
    @PostMapping("/teacher/generate-summary")
    public Map<String, Object> generateSummary(@RequestParam(name = "period", defaultValue = "DAILY") String period,
                                                HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            boolean generated = alertSummaryService.generateSummaryForTeacher(teacherId, period);
            String label = "DAILY".equals(period) ? "每日" : "每周";
            if (generated) {
                return successResult("已生成" + label + "汇总报告");
            } else {
                return errorResult("暂无学生数据，无法生成" + label + "汇总报告。请确认已绑定班级且存在学生记录。");
            }
        } catch (Exception e) {
            return errorResult("生成汇总失败：" + e.getMessage());
        }
    }

    /** POST /alert/teacher/generate-all (手动触发全量预警分析) */
    @PostMapping("/teacher/generate-all")
    public Map<String, Object> generateAllWarnings(HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            int count = warningGenerationService.generateAllWarnings();
            return successResult("全量分析完成，共生成 " + count + " 条预警", Map.of("count", count));
        } catch (Exception e) {
            return errorResult("全量分析失败：" + e.getMessage());
        }
    }

    /**
     * POST /alert/teacher/generate-snapshots (为本教师学生生成"当日全量快照")
     *
     * 与生成预警不同：无论学生当天是否触发预警都会写一条当天快照（GREEN 也写），
     * 一人一课一天只保留一条，用于填充风险趋势曲线。
     */
    @PostMapping("/teacher/generate-snapshots")
    public Map<String, Object> generateSnapshots(HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            int count = warningGenerationService.generateSnapshotsForTeacher(teacherId);
            return successResult("已生成当日快照 " + count + " 条", Map.of("count", count));
        } catch (Exception e) {
            return errorResult("生成当日快照失败：" + e.getMessage());
        }
    }

    /** POST /alert/teacher/generate-snapshots-all (为全体学生生成"当日全量快照"，演示/管理用) */
    @PostMapping("/teacher/generate-snapshots-all")
    public Map<String, Object> generateAllSnapshots(HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            int count = warningGenerationService.generateDailySnapshots();
            return successResult("已生成全体当日快照 " + count + " 条", Map.of("count", count));
        } catch (Exception e) {
            return errorResult("生成全体当日快照失败：" + e.getMessage());
        }
    }

    /** POST /alert/teacher/handle (处理预警+干预措施+同步发送练习题) */
    @PostMapping("/teacher/handle")
    public Map<String, Object> handleAlert(@Valid @RequestBody HandleRequest req, HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            boolean success = alertQueryService.handleAlert(
                    req.getAlertId(), teacherId, req.getNote(),
                    req.getInterventionType(), req.getInterventionDesc(),
                    req.getExerciseIds());
            return success ? successResult("处理成功") : errorResult("处理失败：预警不存在或状态不允许");
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** POST /alert/teacher/dismiss */
    @PostMapping("/teacher/dismiss")
    public Map<String, Object> dismissAlert(@Valid @RequestBody DismissRequest req, HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            boolean success = alertQueryService.dismissAlert(req.getAlertId(), teacherId, req.getReason());
            return success ? successResult("撤销成功") : errorResult("撤销失败：预警不存在或状态不允许");
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** POST /alert/teacher/close (闭环完成) */
    @PostMapping("/teacher/close")
    public Map<String, Object> closeAlert(@Valid @RequestBody CloseRequest req, HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            boolean success = alertQueryService.closeAlert(req.getAlertId(), teacherId, req.getNote());
            return success ? successResult("闭环完成") : errorResult("操作失败：预警不存在或状态不允许");
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** POST /alert/teacher/batch-handle (批量处理) */
    @PostMapping("/teacher/batch-handle")
    public Map<String, Object> batchHandle(@Valid @RequestBody BatchRequest req, HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            int count = alertQueryService.batchHandle(req.getAlertIds(), teacherId, req.getNote(),
                    req.getInterventionType(), req.getInterventionDesc());
            return successResult("成功处理 " + count + " 条预警", Map.of("count", count));
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** POST /alert/teacher/batch-dismiss (批量撤销) */
    @PostMapping("/teacher/batch-dismiss")
    public Map<String, Object> batchDismiss(@Valid @RequestBody BatchRequest req, HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            int count = alertQueryService.batchDismiss(req.getAlertIds(), teacherId, req.getNote());
            return successResult("成功撤销 " + count + " 条预警", Map.of("count", count));
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** POST /alert/teacher/batch-close (批量关闭) */
    @PostMapping("/teacher/batch-close")
    public Map<String, Object> batchClose(@Valid @RequestBody BatchRequest req, HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            int count = alertQueryService.batchClose(req.getAlertIds(), teacherId, req.getNote());
            return successResult("成功关闭 " + count + " 条预警", Map.of("count", count));
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** POST /alert/teacher/reopen (重新打开) */
    @PostMapping("/teacher/reopen")
    public Map<String, Object> reopenAlert(@Valid @RequestBody ReopenRequest req, HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            boolean success = alertQueryService.reopenAlert(req.getAlertId(), teacherId, req.getReason());
            return success ? successResult("已重新打开") : errorResult("操作失败：预警不存在或状态不允许");
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** GET /alert/{alertId}/available-exercises (处理预警时获取可选题列表) */
    @GetMapping("/{alertId}/available-exercises")
    public Map<String, Object> getAvailableExercises(@PathVariable Long alertId,
                                                      HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            List<Map<String, Object>> exercises = alertQueryService.getAvailableExercisesForAlert(alertId);
            Map<String, Object> result = successResult();
            result.put("data", exercises);
            result.put("total", exercises.size());
            return result;
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** POST /alert/teacher/intervention (创建干预记录) */
    @PostMapping("/teacher/intervention")
    public Map<String, Object> createIntervention(@Valid @RequestBody InterventionRequest req,
                                                   HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            InterventionRecord record = interventionService.create(
                    req.getAlertId(), req.getStudentId(), teacherId,
                    req.getInterventionType(), req.getDescription(),
                    req.getRiskScoreBefore());
            return successResult("干预记录已创建", Map.of("id", record.getId()));
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** PUT /alert/teacher/intervention/{id} (更新干预状态) */
    @PutMapping("/teacher/intervention/{id}")
    public Map<String, Object> updateIntervention(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateInterventionRequest req,
                                                   HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            boolean success = interventionService.updateStatus(id, req.getStatus(), req.getResultNote());
            return success ? successResult("更新成功") : errorResult("干预记录不存在");
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** POST /alert/teacher/intervention/{id}/check (效果回测) */
    @PostMapping("/teacher/intervention/{id}/check")
    public Map<String, Object> checkInterventionEffect(@PathVariable Long id,
                                                        HttpServletRequest request) {
        Long teacherId = resolveTeacherId(request);
        if (teacherId == null) return errorResult("未登录");
        try {
            BigDecimal change = interventionService.checkEffect(id);
            return successResult("效果检查完成",
                    Map.of("riskChange", change, "improved", change.doubleValue() < 0));
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** GET /alert/{id}/detail (预警详情+干预+日志) */
    @GetMapping("/{id}/detail")
    public Map<String, Object> getAlertDetail(@PathVariable Long id, HttpServletRequest request) {
        if (resolveTeacherId(request) == null && resolveStudentId(request) == null)
            return errorResult("未登录");
        try {
            AlertRecordVO detail = alertQueryService.getAlertDetail(id);
            if (detail == null) return errorResult("预警不存在");
            Map<String, Object> result = successResult();
            result.put("data", detail);
            return result;
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** GET /alert/{id}/interventions (干预记录列表) */
    @GetMapping("/{id}/interventions")
    public Map<String, Object> getInterventions(@PathVariable Long id, HttpServletRequest request) {
        if (resolveTeacherId(request) == null && resolveStudentId(request) == null)
            return errorResult("未登录");
        try {
            List<InterventionRecord> list = interventionService.getByAlertId(id);
            return successPage(list, list.size());
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** GET /alert/{id}/logs (操作日志) */
    @GetMapping("/{id}/logs")
    public Map<String, Object> getOperationLogs(@PathVariable Long id, HttpServletRequest request) {
        if (resolveTeacherId(request) == null && resolveStudentId(request) == null)
            return errorResult("未登录");
        try {
            List<AlertOperationLog> list = alertQueryService.getOperationLogs(id);
            return successPage(list, list.size());
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    // ==================== 通知中心 ====================

    /** GET /alert/notifications (通知列表) */
    @GetMapping("/notifications")
    public Map<String, Object> getNotifications(@RequestParam(defaultValue = "TEACHER") String type,
                                                 HttpServletRequest request) {
        Long userId = "STUDENT".equals(type) ? resolveStudentId(request) : resolveTeacherId(request);
        if (userId == null) return errorResult("未登录");
        try {
            List<Notification> list = notificationService.getList(userId, type);
            Map<String, Object> r = successResult();
            r.put("data", list);
            r.put("total", list.size());
            return r;
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** GET /alert/notifications/unread-count (未读数) */
    @GetMapping("/notifications/unread-count")
    public Map<String, Object> getUnreadCount(@RequestParam(defaultValue = "TEACHER") String type,
                                               HttpServletRequest request) {
        Long userId = "STUDENT".equals(type) ? resolveStudentId(request) : resolveTeacherId(request);
        if (userId == null) return errorResult("未登录");
        try {
            long count = notificationService.getUnreadCount(userId, type);
            Map<String, Object> r = successResult();
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("count", count);
            r.put("data", d);
            return r;
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** PUT /alert/notifications/{id}/read (标记已读) */
    @PutMapping("/notifications/{id}/read")
    public Map<String, Object> markRead(@PathVariable Long id,
                                         @RequestParam(defaultValue = "TEACHER") String type,
                                         HttpServletRequest request) {
        Long userId = "STUDENT".equals(type) ? resolveStudentId(request) : resolveTeacherId(request);
        if (userId == null) return errorResult("未登录");
        notificationService.markRead(id);
        return successResult("ok");
    }

    /** PUT /alert/notifications/read-all (全部已读) */
    @PutMapping("/notifications/read-all")
    public Map<String, Object> markAllRead(@RequestParam(defaultValue = "TEACHER") String type,
                                            HttpServletRequest request) {
        Long userId = "STUDENT".equals(type) ? resolveStudentId(request) : resolveTeacherId(request);
        if (userId == null) return errorResult("未登录");
        notificationService.markAllRead(userId, type);
        return successResult("ok");
    }

    // ==================== 学生端 ====================

    /** GET /alert/student/list */
    @GetMapping("/student/list")
    public Map<String, Object> getStudentAlerts(@RequestParam(required = false) String status,
                                                 HttpServletRequest request) {
        Long studentId = resolveStudentId(request);
        if (studentId == null) return errorResult("未登录");
        try {
            List<AlertRecordVO> alerts = alertQueryService.getAlertsByStudentId(studentId, status);
            return successPage(alerts, alerts.size());
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** POST /alert/student/acknowledge */
    @PostMapping("/student/acknowledge")
    public Map<String, Object> acknowledgeAlert(@Valid @RequestBody AcknowledgeRequest req,
                                                 HttpServletRequest request) {
        Long studentId = resolveStudentId(request);
        if (studentId == null) return errorResult("未登录");
        try {
            boolean success = alertQueryService.acknowledgeAlert(req.getAlertId(), studentId);
            return success ? successResult("确认成功") : errorResult("确认失败：预警不存在或不属于当前学生");
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /** POST /alert/student/respond (学生回应老师处理意见) */
    @PostMapping("/student/respond")
    public Map<String, Object> studentRespond(@Valid @RequestBody RespondRequest req,
                                               HttpServletRequest request) {
        Long studentId = resolveStudentId(request);
        if (studentId == null) return errorResult("未登录");
        try {
            boolean success = alertQueryService.studentRespondToAlert(
                    req.getAlertId(), studentId, req.getResponse());
            return success ? successResult("回应成功，老师将收到反馈")
                    : errorResult("回应失败：预警不存在或状态不允许");
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    // ==================== 鉴权辅助 ====================

    private Long resolveTeacherId(HttpServletRequest request) {
        String token = extractBearerToken(request.getHeader("Authorization"));
        Long teacherId = tokenService.validateAndGetTeacherId(token);
        if (teacherId != null) return teacherId;
        String h = request.getHeader("X-Teacher-Id");
        if (h != null && !h.isBlank()) {
            try { return Long.parseLong(h.trim()); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private Long resolveStudentId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) return null;
        if (auth.startsWith("Bearer ")) auth = auth.substring(7).trim();
        return tokenService.validateAndGetTeacherId(auth);
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) return null;
        if (authorization.startsWith("Bearer ")) return authorization.substring(7).trim();
        return authorization.trim();
    }

    // ==================== 响应辅助 ====================

    private Map<String, Object> successResult() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true); r.put("message", "操作成功"); return r;
    }

    private Map<String, Object> successResult(String msg) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true); r.put("message", msg); return r;
    }

    private Map<String, Object> successResult(String msg, Map<String, Object> extra) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true); r.put("message", msg); r.putAll(extra); return r;
    }

    private Map<String, Object> successPage(List<?> data, int total) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true); r.put("message", "查询成功");
        r.put("data", data); r.put("total", total); return r;
    }

    private Map<String, Object> errorResult(String message) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false); r.put("message", message); return r;
    }

    // ==================== 请求体内部类 ====================

    public static class HandleRequest {
        @NotNull private Long alertId;
        private String note;
        private String interventionType;
        private String interventionDesc;
        private List<Long> exerciseIds;  // 同步发送的练习题ID列表

        public Long getAlertId() { return alertId; }
        public void setAlertId(Long alertId) { this.alertId = alertId; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public String getInterventionType() { return interventionType; }
        public void setInterventionType(String t) { this.interventionType = t; }
        public String getInterventionDesc() { return interventionDesc; }
        public void setInterventionDesc(String d) { this.interventionDesc = d; }
        public List<Long> getExerciseIds() { return exerciseIds; }
        public void setExerciseIds(List<Long> ids) { this.exerciseIds = ids; }
    }

    public static class DismissRequest {
        @NotNull private Long alertId;
        @NotBlank private String reason;
        public Long getAlertId() { return alertId; }
        public void setAlertId(Long alertId) { this.alertId = alertId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class CloseRequest {
        @NotNull private Long alertId;
        private String note;
        public Long getAlertId() { return alertId; }
        public void setAlertId(Long alertId) { this.alertId = alertId; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    public static class ReopenRequest {
        @NotNull private Long alertId;
        @NotBlank private String reason;
        public Long getAlertId() { return alertId; }
        public void setAlertId(Long alertId) { this.alertId = alertId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class AcknowledgeRequest {
        @NotNull private Long alertId;
        public Long getAlertId() { return alertId; }
        public void setAlertId(Long alertId) { this.alertId = alertId; }
    }

    public static class RespondRequest {
        @NotNull private Long alertId;
        @NotBlank private String response;
        public Long getAlertId() { return alertId; }
        public void setAlertId(Long alertId) { this.alertId = alertId; }
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
    }

    public static class BatchRequest {
        @NotEmpty private List<Long> alertIds;
        private String note;
        private String interventionType;
        private String interventionDesc;
        public List<Long> getAlertIds() { return alertIds; }
        public void setAlertIds(List<Long> alertIds) { this.alertIds = alertIds; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public String getInterventionType() { return interventionType; }
        public void setInterventionType(String t) { this.interventionType = t; }
        public String getInterventionDesc() { return interventionDesc; }
        public void setInterventionDesc(String d) { this.interventionDesc = d; }
    }

    public static class InterventionRequest {
        @NotNull private Long alertId;
        @NotNull private Long studentId;
        @NotBlank private String interventionType;
        private String description;
        private BigDecimal riskScoreBefore;

        public Long getAlertId() { return alertId; }
        public void setAlertId(Long id) { this.alertId = id; }
        public Long getStudentId() { return studentId; }
        public void setStudentId(Long id) { this.studentId = id; }
        public String getInterventionType() { return interventionType; }
        public void setInterventionType(String t) { this.interventionType = t; }
        public String getDescription() { return description; }
        public void setDescription(String d) { this.description = d; }
        public BigDecimal getRiskScoreBefore() { return riskScoreBefore; }
        public void setRiskScoreBefore(BigDecimal s) { this.riskScoreBefore = s; }
    }

    public static class UpdateInterventionRequest {
        @NotBlank private String status;
        private String resultNote;
        public String getStatus() { return status; }
        public void setStatus(String s) { this.status = s; }
        public String getResultNote() { return resultNote; }
        public void setResultNote(String n) { this.resultNote = n; }
    }
}
