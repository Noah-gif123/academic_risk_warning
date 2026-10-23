package com.example.academic_risk_warning.controller.teacher;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.AlertEventService;
import com.example.academic_risk_warning.vo.AlertRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教师端 · 预警事件（第二阶段第 2 步）。
 *
 * <p>数据来源：alert_record（一人一课一条当前预警）+ alert_flow_log（时间线）+ intervention_record（处置记录）。
 * 教师只能操作自己教学班的预警。</p>
 */
@RestController
@RequestMapping("/api/teacher/alerts")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherAlertController {

    private final AlertEventService alertEventService;

    /** 预警列表：可按开课/教学班/等级/状态过滤；appealPending=true 只看待核实申诉 */
    @GetMapping
    public Result<List<AlertRowVO>> list(@RequestParam(required = false) Long offeringId,
                                         @RequestParam(required = false) Long teachingClassId,
                                         @RequestParam(required = false) String level,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) Boolean appealPending) {
        return Result.ok(alertEventService.listForTeacher(UserContext.require(), offeringId, teachingClassId,
                level, status, appealPending));
    }

    /** 预警总览计数：总数、红橙黄、待处理/处理中/已关闭、已升级、今日新增、最近判定 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam(required = false) Long teachingClassId) {
        return Result.ok(alertEventService.stats(UserContext.require(), teachingClassId));
    }

    /** 导出预警名单（xlsx，字段与列表页一致） */
    @GetMapping("/export")
    public org.springframework.http.ResponseEntity<byte[]> export(@RequestParam(required = false) Long teachingClassId,
                                                                @RequestParam(required = false) String level,
                                                                @RequestParam(required = false) String status) {
        byte[] body = alertEventService.exportForTeacher(UserContext.require(), teachingClassId, level, status);
        String name = "预警名单.xlsx";
        String encoded;
        try {
            encoded = java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            encoded = "alerts.xlsx";
        }
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    /** 预警详情：预警本体 + 时间线 + 处置记录 + 当前风险状态 */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(alertEventService.detail(UserContext.require(), id));
    }

    /** 认领：ACTIVE → HANDLING */
    @PostMapping("/{id}/claim")
    public Result<AlertRowVO> claim(@PathVariable Long id) {
        return Result.ok(alertEventService.claim(UserContext.require(), id));
    }

    /** 记录处理（写干预记录，stage=TEACHER） */
    @PostMapping("/{id}/handle")
    public Result<AlertRowVO> handle(@PathVariable Long id, @RequestBody HandleRequest request) {
        return Result.ok(alertEventService.handle(UserContext.require(), id,
                request.getActionType(), request.getContent(), request.getResult()));
    }

    /** 关闭（填写结论） */
    @PostMapping("/{id}/close")
    public Result<AlertRowVO> close(@PathVariable Long id, @RequestBody(required = false) NoteRequest request) {
        return Result.ok(alertEventService.close(UserContext.require(), id,
                request == null ? null : request.getNote()));
    }

    /** 忽略（必须填原因） */
    @PostMapping("/{id}/dismiss")
    public Result<AlertRowVO> dismiss(@PathVariable Long id, @RequestBody NoteRequest request) {
        return Result.ok(alertEventService.dismiss(UserContext.require(), id,
                request == null ? null : request.getReason()));
    }

    /** 提醒学生（站内消息，同一预警同一天只发一条） */
    @PostMapping("/{id}/remind")
    public Result<Integer> remind(@PathVariable Long id, @RequestBody(required = false) NoteRequest request) {
        return Result.ok(alertEventService.remindStudent(UserContext.require(), id,
                request == null ? null : request.getNote()));
    }

    /** 回复申诉（学生端可见，并发一条站内消息） */
    @PostMapping("/{id}/reply")
    public Result<AlertRowVO> reply(@PathVariable Long id, @RequestBody NoteRequest request) {
        return Result.ok(alertEventService.replyToAppeal(UserContext.require(), id,
                request == null ? null : request.getReply()));
    }

    /** 处理请求体 */
    @lombok.Data
    public static class HandleRequest {
        private String actionType;
        private String content;
        private String result;
    }

    /** 备注/原因请求体（关闭用 note，忽略用 reason，提醒用 note，回复用 reply） */
    @lombok.Data
    public static class NoteRequest {
        private String note;
        private String reason;
        private String reply;
    }
}
