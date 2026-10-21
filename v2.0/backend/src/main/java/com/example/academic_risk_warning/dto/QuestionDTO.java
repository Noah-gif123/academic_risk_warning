package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 题目（教师保存 / 导入校对共用） */
@Data
public class QuestionDTO {
    private Long id;
    private Integer qNo;
    /** SINGLE / MULTI / JUDGE / BLANK / SHORT */
    private String qType;
    private String stem;
    private String optionsJson;
    private String answerJson;
    private String analysis;
    private BigDecimal fullScore;
    private String groupName;
    private Integer sortNo;
}
