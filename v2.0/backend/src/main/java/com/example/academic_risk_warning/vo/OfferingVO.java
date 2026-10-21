package com.example.academic_risk_warning.vo;

import lombok.Data;

/** 开课实例视图 */
@Data
public class OfferingVO {
    private Long id;
    private Long courseId;
    private String courseName;
    private String courseType;
    private Long termId;
    private String termName;
    private Integer plannedWeeks;
    private Integer plannedTimesPerWeek;
    private Integer plannedSessions;
    private Integer actualSessions;
    private Integer effectiveSessions;
    private String effectiveSource;
    private Integer passScore;
    private String status;
    private Integer classCount;
    private Integer enrollCount;
    private Integer sessionCount;
}
