package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 手工评分项的一行录入 */
@Data
public class ScoreEntryDTO {
    private Long studentId;
    private BigDecimal score;
    /** 等级制 / 通过制的原始取值，如 优秀 / 通过 */
    private String rawValue;
    /** 缺交/缺考：按 0 分计（与「还没录入」区分） */
    private Integer isAbsent;
    private String remark;
}
