package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 学生问卷作答（匿名，只提交分数） */
@Data
public class QuestionnaireAnswerRequest {
    private List<Item> items;

    @Data
    public static class Item {
        private String itemCode;
        private BigDecimal score;
        private String text;
    }
}
