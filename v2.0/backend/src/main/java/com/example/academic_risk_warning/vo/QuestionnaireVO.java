package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 问卷（当前只有「教师风格判定」一种，匿名） */
@Data
public class QuestionnaireVO {
    private Long id;
    private String title;
    private String questionnaireType;
    private String questionnaireTypeText;
    private Long adminClassId;
    private String adminClassName;
    private String courseName;
    private Boolean anonymous;
    private String status;
    private String statusText;
    private Integer answerCount;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private String collectorType;
    private LocalDateTime createTime;
}
