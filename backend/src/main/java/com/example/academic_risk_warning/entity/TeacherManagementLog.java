package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师管理记录表实体
 */
@Data
@TableName("teacher_management_log")
public class TeacherManagementLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被管理的教师ID */
    private Long teacherId;

    /** 操作的管理员ID */
    private Long adminId;

    /** 操作类型 */
    private String actionType;

    /** 变更前教师管理状态 */
    private String oldStatus;

    /** 变更后教师管理状态 */
    private String newStatus;

    /** 管理备注 */
    private String remark;

    /** 操作时间 */
    private LocalDateTime createTime;
}
