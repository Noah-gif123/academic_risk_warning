package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeacherManagementLog;
import com.example.academic_risk_warning.mapper.TeacherManagementLogMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.mapper.NotificationMapper;
import com.example.academic_risk_warning.entity.Notification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教师管理操作服务
 */
@Service
public class TeacherManagementService {

    private final TeacherManagementLogMapper managementLogMapper;
    private final TeacherMapper teacherMapper;
    private final NotificationMapper notificationMapper;

    public TeacherManagementService(TeacherManagementLogMapper managementLogMapper,
                                    TeacherMapper teacherMapper,
                                    NotificationMapper notificationMapper) {
        this.managementLogMapper = managementLogMapper;
        this.teacherMapper = teacherMapper;
        this.notificationMapper = notificationMapper;
    }

    /**
     * 标记教师管理状态
     */
    public void markStatus(Long adminId, Long teacherId, String newStatus, String remark) {
        String oldStatus = getCurrentStatus(teacherId);

        TeacherManagementLog log = new TeacherManagementLog();
        log.setTeacherId(teacherId);
        log.setAdminId(adminId);

        String actionType = switch (newStatus) {
            case "ATTENTION" -> "MARK_ATTENTION";
            case "WARNING" -> "MARK_WARNING";
            case "RECTIFY" -> "MARK_RECTIFY";
            case "NORMAL" -> "RESOLVE";
            case "RESOLVED" -> "RESOLVE";
            default -> "ADD_REMARK";
        };
        log.setActionType(actionType);
        log.setOldStatus(oldStatus);
        log.setNewStatus(
                "RESOLVE".equals(actionType) || "NORMAL".equals(newStatus) ? "NORMAL" : newStatus);
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        managementLogMapper.insert(log);
    }

    /**
     * 向教师发送管理通知
     */
    public void sendNotice(Long adminId, Long teacherId, String title, String content) {
        // 记录操作日志
        TeacherManagementLog log = new TeacherManagementLog();
        log.setTeacherId(teacherId);
        log.setAdminId(adminId);
        log.setActionType("SEND_NOTICE");
        log.setOldStatus(getCurrentStatus(teacherId));
        log.setNewStatus(getCurrentStatus(teacherId));
        log.setRemark(title + ": " + content);
        log.setCreateTime(LocalDateTime.now());
        managementLogMapper.insert(log);

        // 发送通知
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher != null) {
            Notification n = new Notification();
            n.setRecipientId(teacherId);
            n.setRecipientType("TEACHER");
            n.setTitle(title);
            n.setContent(content);
            n.setType("ADMIN_NOTICE");
            n.setChannel("IN_APP");
            n.setIsRead(false);
            n.setSendStatus("SENT");
            n.setSendTime(LocalDateTime.now());
            n.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(n);
        }
    }

    /**
     * 添加管理备注
     */
    public void addRemark(Long adminId, Long teacherId, String remark) {
        TeacherManagementLog log = new TeacherManagementLog();
        log.setTeacherId(teacherId);
        log.setAdminId(adminId);
        log.setActionType("ADD_REMARK");
        log.setOldStatus(getCurrentStatus(teacherId));
        log.setNewStatus(getCurrentStatus(teacherId));
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        managementLogMapper.insert(log);
    }

    /**
     * 获取教师的管理日志
     */
    public List<TeacherManagementLog> getLogs(Long teacherId) {
        return managementLogMapper.selectList(
                new LambdaQueryWrapper<TeacherManagementLog>()
                        .eq(TeacherManagementLog::getTeacherId, teacherId)
                        .orderByDesc(TeacherManagementLog::getCreateTime));
    }

    /**
     * 获取全部管理操作日志（最近50条）
     */
    public List<TeacherManagementLog> getAllLogs() {
        Page<TeacherManagementLog> p = new Page<>(1, 50);
        Page<TeacherManagementLog> result = managementLogMapper.selectPage(p,
                new LambdaQueryWrapper<TeacherManagementLog>()
                        .orderByDesc(TeacherManagementLog::getCreateTime));
        return result.getRecords();
    }

    /**
     * 获取教师当前管理状态
     */
    private String getCurrentStatus(Long teacherId) {
        List<TeacherManagementLog> logs = managementLogMapper.selectList(
                new LambdaQueryWrapper<TeacherManagementLog>()
                        .eq(TeacherManagementLog::getTeacherId, teacherId)
                        .orderByDesc(TeacherManagementLog::getCreateTime)
                        .last("LIMIT 1"));
        if (logs.isEmpty()) return "NORMAL";
        return logs.get(0).getNewStatus() != null ? logs.get(0).getNewStatus() : "NORMAL";
    }
}
