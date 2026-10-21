package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 导员首页看板 */
@Data
public class CounselorOverviewVO {
    private String termName;
    private Integer weekNo;
    private Integer classCount;
    private Integer studentCount;
    private Integer riskStudentCount;
    private Integer failStudentCount;
    private Integer hardFailStudentCount;
    private BigDecimal avgFailRate;
    private List<CounselorClassSummaryVO> classes;
    private List<CounselorStudentVO> focusStudents;
    private List<String> reminders;
}
