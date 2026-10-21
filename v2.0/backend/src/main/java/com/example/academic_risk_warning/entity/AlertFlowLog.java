package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 预警生命周期时间线（替代快照曲线）
 * <p>对应表：alert_flow_log</p>
 */
@Data
@TableName("alert_flow_log")
public class AlertFlowLog {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 预警ID */
    private Long alertId;

    /** 学生ID */
    private Long studentId;

    /** 学期ID */
    private Long termId;

    /** 事件：CREATED生成/LEVEL_UP升级/LEVEL_DOWN降级/NOTIFIED已通知/ACKNOWLEDGED已知悉/HANDLED已处理/CLOSED关闭/DISMISSED忽略/EXPIRED超时/ESCALATED已升级上报/REFERRED上报教务/ADMIN_HANDLED教务处理/TERMINATED终止干预/REOPENED重新打开/VOIDED作废 */
    private String eventType;

    /** 变更前等级 */
    private String fromLevel;

    /** 变更后等级 */
    private String toLevel;

    /** 变更前状态 */
    private String fromStatus;

    /** 变更后状态 */
    private String toStatus;

    /** 事件说明（如"缺课达 11 次，触发 1/3 硬门槛"） */
    private String detail;

    /** 操作人ID（系统触发为空） */
    private Long operatorId;

    /** 操作人角色：TEACHER/COUNSELOR/ADMIN/SYSTEM */
    private String operatorRole;

    /** 事件时间 */
    private LocalDateTime eventTime;

    private LocalDateTime createTime;
}
