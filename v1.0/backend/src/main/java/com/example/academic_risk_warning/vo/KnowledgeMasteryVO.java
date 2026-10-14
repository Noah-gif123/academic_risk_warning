package com.example.academic_risk_warning.vo;

import lombok.Data;

@Data
public class KnowledgeMasteryVO {

    private Long id;
    private Long courseId;
    private String courseName;
    private Integer totalKnowledge;
    private Integer errorCount;
    private Integer totalQuestion;
    private Integer weakKnowledgeCount;
    private Integer basicTotal;
    private Integer basicCorrect;
}
