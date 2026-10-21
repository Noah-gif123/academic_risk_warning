package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 通知投递与回执表（每个接收人一行）
 * <p>对应表：notification_receipt</p>
 */
@Data
@TableName("notification_receipt")
public class NotificationReceipt {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 通知ID */
    private Long notificationId;

    /** 接收人ID（sys_account.id） */
    private Long recipientId;

    /** 接收人角色：STUDENT/TEACHER/COUNSELOR/ADMIN */
    private String recipientRole;

    /** 渠道：IN_APP站内信/EMAIL邮件/SMS短信 */
    private String channel;

    /** 发送状态：PENDING待发/SENT已发/FAILED失败 */
    private String sendStatus;

    /** 发送时间 */
    private LocalDateTime sendTime;

    /** 是否已读 */
    private Integer isRead;

    /** 已读时间 */
    private LocalDateTime readTime;

    /** 是否需要回执确认 */
    private Integer ackRequired;

    /** 回执状态：NONE无需/PENDING待确认/ACKED已确认/IGNORED已忽略 */
    private String ackStatus;

    /** 确认时间 */
    private LocalDateTime ackTime;

    /** 确认说明 */
    private String ackNote;

    /** 已采取的行动（如"已开班会"、"已约谈学生"） */
    private String actionTaken;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
