package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 成绩矩阵的一列 = 一个叶子评分项 */
@Data
public class ScoreItemColumnVO {
    private Long schemeItemId;
    private String itemName;
    private String path;
    private String category;
    private BigDecimal weight;
    private String dataSource;
    private String scoreType;
    private BigDecimal fullScore;
    private Integer expectedTimes;
    private Long parentId;
}
