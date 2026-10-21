package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 贡献表一行：某成员占多少比例（0~1，合计必须 1） */
@Data
public class ContributionItemDTO {
    private Long studentId;
    private BigDecimal ratio;
    private String note;
}
