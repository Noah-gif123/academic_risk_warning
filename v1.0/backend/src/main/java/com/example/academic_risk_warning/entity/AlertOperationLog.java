package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预警操作审计日志
 */
@Data
@TableName("alert_operation_log")
public class AlertOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long alertId;

    private Long operatorId;

    /** TEACHER/STUDENT/SYSTEM */
    private String operatorType;

    /** GENERATE/ACKNOWLEDGE/HANDLE/DISMISS/ARCHIVE/REOPEN/CLOSE */
    private String operation;

    private String fromStatus;

    private String toStatus;

    private String remark;

    private LocalDateTime createTime;
}
