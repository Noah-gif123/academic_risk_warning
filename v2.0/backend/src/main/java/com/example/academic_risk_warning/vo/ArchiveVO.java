package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 归档批次视图 */
@Data
public class ArchiveVO {
    private Long id;
    private Long offeringId;
    private Long teachingClassId;
    private String classCode;
    private String className;
    private String courseName;
    private String termName;
    private String teacherName;
    private String phase;
    private String status;
    private LocalDateTime submitTime;
    private String submitNote;
    private Integer sessionFrom;
    private Integer sessionTo;
    private Integer sessionCount;
    private Integer recordCount;
    private BigDecimal coverageRate;
    private LocalDateTime reviewTime;
    private String reviewNote;
    private LocalDateTime returnTime;
    private String returnReason;
    private Integer remindCount;
    private LocalDateTime deadline;
}
