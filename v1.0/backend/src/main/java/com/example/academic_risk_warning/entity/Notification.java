package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recipientId;

    /** TEACHER / STUDENT */
    private String recipientType;

    private String title;

    private String content;

    private Long alertId;

    /** ALERT_NEW / ALERT_UPGRADE / HANDLE_RESULT / STUDENT_RESPOND / TIMEOUT / SYSTEM */
    private String type;

    /** IN_APP / EMAIL / SMS */
    private String channel;

    private Boolean isRead;

    private LocalDateTime readTime;

    /** PENDING / SENT / FAILED */
    private String sendStatus;

    private LocalDateTime sendTime;

    private LocalDateTime createTime;

    /** 跳转目标类型: ALERT / RECOMMEND / COURSE */
    private String targetType;

    /** 跳转目标ID: 对应的预警ID、推荐ID 或 课程ID */
    private Long targetId;
}
