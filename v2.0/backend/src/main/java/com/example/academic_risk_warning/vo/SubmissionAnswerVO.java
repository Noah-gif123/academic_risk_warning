package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 单题作答与判分 */
@Data
public class SubmissionAnswerVO {
    private Long id;
    private Long questionId;
    private Integer qNo;
    private String qType;
    private String stem;
    private String optionsJson;
    private BigDecimal fullScore;
    private String answerJson;
    private String stdAnswerJson;
    private String analysis;
    private BigDecimal score;
    private Integer isCorrect;
    private String comment;
    private String markedByType;
    private Long markedBy;
    private String markedByName;
    private LocalDateTime markedAt;
}
