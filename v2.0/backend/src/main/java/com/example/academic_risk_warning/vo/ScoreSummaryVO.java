package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 矩阵 / 总评的汇总信息 */
@Data
public class ScoreSummaryVO {
    private Integer studentCount;
    private Integer scoredCount;
    private Integer incompleteCount;
    private Integer failCount;
    private Integer hardFailCount;
    private BigDecimal passRate;
    private BigDecimal avgTotal;
    private BigDecimal maxTotal;
    private BigDecimal minTotal;
    private Integer passScore;
    private Boolean schemeLocked;
    private String schemeVersion;
    private List<String> missingItemNames;
}
