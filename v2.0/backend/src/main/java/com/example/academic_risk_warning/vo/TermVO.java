package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.time.LocalDate;

/** 学期视图：带“是否当前学期”与周历情况 */
@Data
public class TermVO {
    private Long id;
    private String termCode;
    private String schoolYear;
    private Integer termNo;
    private String termName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalWeeks;
    private boolean current;
    private String status;
    private Integer calendarWeeks;
}
