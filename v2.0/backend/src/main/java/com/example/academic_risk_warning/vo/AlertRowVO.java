package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 教师端预警列表的一行（预警监控页使用） */
@Data
public class AlertRowVO {
    private Long id;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Long enrollmentId;
    private Long offeringId;
    private String courseName;
    private Long teachingClassId;
    private String classCode;
    private String className;

    private String alertLevel;
    private String triggerChannel;
    private String triggerRule;
    private BigDecimal riskScore;
    private BigDecimal confidence;
    private String outputMode;
    private String alertReason;

    private String status;
    private Integer isCurrent;
    private Integer escalated;
    private String previousLevel;

    private Integer notified;
    private LocalDateTime firstNotifyTime;
    private LocalDateTime expireTime;

    private Long handleBy;
    private String handleRole;
    private LocalDateTime handleTime;
    private String handleNote;
    private LocalDateTime closeTime;
    private String closeNote;

    /** 学生申诉说明（学生端提交、教师端核实） */
    private String studentResponse;
    private LocalDateTime studentResponseTime;
    /** 教师回复（学生端可见）；为空表示申诉待核实 */
    private String teacherReply;
    private LocalDateTime teacherReplyTime;

    private String engineVersion;
    private LocalDateTime createTime;

    /** A/B 通道的关键数字，页面详情直接用 */
    private Integer absentCount;
    private Integer allowedAbsent;
    private Integer remainingAbsent;
    private Integer hardFail;
    private Integer nearFail;
    private BigDecimal finalExamNeeded;
    private Integer finalUnreachable;
    private String channelALevel;
    private BigDecimal channelBScore;
    private Integer channelBRank;
    private Integer channelBClassSize;
}
