package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 通知与回执 */
@Data
public class CounselorNoticeVO {
    private Long id;
    private String bizType;
    private Long bizId;
    private String title;
    private String content;
    private String audienceDesc;
    private String priority;
    private Integer aggregateCount;
    private LocalDateTime createTime;
    private Integer isRead;
    private LocalDateTime readTime;
}
