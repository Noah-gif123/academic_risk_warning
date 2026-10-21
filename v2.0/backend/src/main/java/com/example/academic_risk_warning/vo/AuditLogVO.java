package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 审计日志（改分、改考勤、归档、退回、上报等关键操作） */
@Data
public class AuditLogVO {
    private Long id;
    private String bizType;
    private Long bizId;
    private String action;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String operatorRoleText;
    private String detail;
    private LocalDateTime createTime;
}
