package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 导员名下行政班的概况 */
@Data
public class CounselorClassSummaryVO {
    private Long adminClassId;
    private String classCode;
    private String className;
    private Integer gradeYear;
    private String majorName;
    private Integer studentCount;
    private Integer scoredCount;
    private Integer failCount;
    private Integer hardFailCount;
    private BigDecimal failRate;
    private Integer absentOverCount;
    private Integer riskStudentCount;
    /** 班集体提醒等级：NONE / SUGGEST（≥30% 且 ≥5 人）/ FOCUS（≥50% 且 ≥10 人） */
    private String classAlertLevel;
    private String classAlertText;
    private Integer weekNo;
}
