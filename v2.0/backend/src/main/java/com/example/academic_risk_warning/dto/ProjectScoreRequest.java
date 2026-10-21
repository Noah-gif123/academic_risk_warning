package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 教师给项目总分（团队项目为整组分） */
@Data
public class ProjectScoreRequest {
    private BigDecimal score;
    private String comment;
}
