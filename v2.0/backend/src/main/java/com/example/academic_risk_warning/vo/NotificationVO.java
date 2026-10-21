package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 消息中心的一条消息（notification + 当前接收人的回执） */
@Data
public class NotificationVO {
    /** 通知ID（notification.id） */
    private Long id;
    /** 回执ID（notification_receipt.id） */
    private Long receiptId;
    /** 业务类型：ALERT_NEW / CLASS_ALERT / ARCHIVE_TASK / SCORE_TASK / QUESTIONNAIRE ... */
    private String bizType;
    /** 关联业务ID（预警ID、归档批次、开课ID、问卷ID 等） */
    private Long bizId;
    /** 标题 */
    private String title;
    /** 正文 */
    private String content;
    /** 结构化载荷（前端用来做跳转参数） */
    private String payloadJson;
    /** 聚合条数：>1 表示这是一条汇总通知 */
    private Integer aggregateCount;
    /** 优先级：HIGH / NORMAL / LOW */
    private String priority;
    /** 接收范围描述 */
    private String audienceDesc;
    /** 是否已读：1 已读 0 未读 */
    private Integer isRead;
    /** 发送时间 */
    private LocalDateTime sendTime;
    /** 已读时间 */
    private LocalDateTime readTime;
    /** 是否需要回执确认 */
    private Integer ackRequired;
    /** 回执状态：NONE / PENDING / ACKED / IGNORED */
    private String ackStatus;
}
