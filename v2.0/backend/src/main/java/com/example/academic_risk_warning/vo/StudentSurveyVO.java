package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 学生端问卷详情（含题目与我的作答，匿名） */
@Data
public class StudentSurveyVO {
    private Long id;
    private String title;
    private Boolean anonymous;
    private LocalDateTime closeTime;
    private Boolean answered;
    private List<Question> questions;
    private Map<String, BigDecimal> myScores;

    @Data
    public static class Question {
        private String itemCode;
        private String text;
        private String type;
    }
}
