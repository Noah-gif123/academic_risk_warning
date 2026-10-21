package com.example.academic_risk_warning.dto;

import lombok.Data;

/** 学生某题作答（answerJson 结构随题型而定） */
@Data
public class AnswerSubmitDTO {
    private Long questionId;
    private String answerJson;
}
