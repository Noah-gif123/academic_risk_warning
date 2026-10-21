package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 考勤改动留痕表（行级 before/after）
 * <p>对应表：attendance_record_log</p>
 */
@Data
@TableName("attendance_record_log")
public class AttendanceRecordLog {

    /** 日志ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 考勤记录ID */
    private Long recordId;

    /** 选课ID */
    private Long enrollmentId;

    /** 学生ID */
    private Long studentId;

    /** 课次ID */
    private Long sessionId;

    /** 变更前状态 */
    private String oldStatus;

    /** 变更后状态 */
    private String newStatus;

    /** 变更前口径（NORMAL/ARCHIVE） */
    private String oldPhase;

    /** 变更后口径 */
    private String newPhase;

    /** 变更类型：TEACHER_INPUT老师首次录入/TEACHER_EDIT老师平时修改/ARCHIVE_CONFIRM归档确认/ADMIN_CORRECT归档后教务更正/VOID作废/RESTORE恢复 */
    private String changeType;

    /** 变更原因 */
    private String reason;

    /** 操作人ID */
    private Long changedBy;

    /** 操作人角色：TEACHER/ADMIN */
    private String changedRole;

    /** 变更时间 */
    private LocalDateTime changedAt;

    /** 关联批次号 */
    private String batchKey;
}
