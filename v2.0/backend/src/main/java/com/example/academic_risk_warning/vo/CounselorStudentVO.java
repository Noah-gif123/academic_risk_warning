package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 导员看到的学生行（跨课程聚合） */
@Data
public class CounselorStudentVO {
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Long adminClassId;
    private String adminClassName;
    /** 有风险的课程数（挂科或触发缺课门槛） */
    private Integer riskCourseCount;
    private Integer failCourseCount;
    private Integer hardFailCourseCount;
    private Integer courseCount;
    private BigDecimal minTotalScore;
    private BigDecimal avgTotalScore;
    private Integer absentTotal;
    /** 危险度（用于排序，越大越危险） */
    private BigDecimal riskScore;
    private String riskHint;
    private Long latestInterventionId;
    private String latestInterventionText;
}
