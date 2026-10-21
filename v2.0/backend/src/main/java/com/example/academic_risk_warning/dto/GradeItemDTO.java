package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 教师对某题的打分 */
@Data
public class GradeItemDTO {
    private Long answerId;
    private BigDecimal score;
    private String comment;
}
