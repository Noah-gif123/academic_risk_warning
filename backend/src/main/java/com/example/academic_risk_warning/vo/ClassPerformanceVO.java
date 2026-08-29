package com.example.academic_risk_warning.vo;

import lombok.Data;

@Data
public class ClassPerformanceVO {

    private Long id;
    private Long courseId;
    private String courseName;
    private Integer totalClassTimes;
    private Integer absentCount;
    private Integer lateCount;
    private Integer quizScore;
    private String qaEvaluate;
    private String attitudeEvaluate;
}
