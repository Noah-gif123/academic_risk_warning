package com.example.academic_risk_warning.vo;

import lombok.Data;

@Data
public class HomeworkInfoVO {

    private Long id;
    private Long courseId;
    private String courseName;
    private Integer totalHomework;
    private Integer submitCount;
    private Integer notSubmitCount;
    private Integer lateSubmitCount;
    private String scoreList;
    private Integer avgScore;
}
