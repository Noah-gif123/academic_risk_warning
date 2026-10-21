package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 考勤明细表（学生×课次，双轨：平时/归档）
 * <p>对应表：attendance_record</p>
 */
@Data
@TableName("attendance_record")
public class AttendanceRecord {

    /** 考勤记录ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 选课ID（学生×教学班×学期） */
    private Long enrollmentId;

    /** 学生ID（由 enrollment 决定、写入时校验一致；学生端按此查询） */
    private Long studentId;

    /** 课次ID */
    private Long sessionId;

    /** 学期ID */
    private Long termId;

    /** 考勤状态：PRESENT出勤/LATE迟到/ABSENT缺课/LEAVE请假（四类必须分开） */
    private String status;

    /** 请假类型：PERSONAL事假/SICK病假/OFFICIAL公假（仅记录，不影响扣分口径） */
    private String leaveType;

    /** 当前口径：NORMAL平时（轨道①）/ARCHIVE归档（轨道②，判定依据） */
    private String phase;

    /** 录入来源：TEACHER老师直录/ADMIN教务统一录入 */
    private String source;

    /** 所属归档批次ID（phase=ARCHIVE 时非空） */
    private Long archiveId;

    /** 录入人ID */
    private Long recordedBy;

    /** 录入人角色：TEACHER/ADMIN */
    private String recordedRole;

    /** 录入时间 */
    private LocalDateTime recordedAt;

    /** 最后修改人 */
    private Long updatedBy;

    /** 最后修改人角色 */
    private String updatedRole;

    /** 修改说明（归档后修改必须填写） */
    private String updateNote;

    /** 录入批次号（一次点名单提交=一个批次，便于整批撤回） */
    private String batchKey;

    /** 是否作废：1作废（整节课误录/退课等），不计入任何统计 */
    private Integer isVoid;

    /** 作废原因 */
    private String voidReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
