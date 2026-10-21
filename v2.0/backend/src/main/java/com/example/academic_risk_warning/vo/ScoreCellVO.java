package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 矩阵单元格：一个学生在一个评分项上的成绩（百分制） */
@Data
public class ScoreCellVO {
    private BigDecimal score;
    private String rawValue;
    private String source;
    private Integer isAbsent;
    private Integer times;
    private Boolean missing;
    private Boolean manual;
    private String note;
}
