package com.example.academic_risk_warning.vo;

import lombok.Data;

@Data
public class ScoreInfoVO {

    private Long id;
    private Long courseId;
    private String courseName;
    private Integer usualScore;
    private Integer midScore;
    private Integer finalScore;
    
    /** 综合成绩（由算法根据学生年级和课程前序关系计算得出） */
    private Double calculatedScore;

    /** 学生整体综合成绩（各科目calculatedScore的平均值） */
    private Double overallScore;
}
