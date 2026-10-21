package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 题目（教师保存 / 导入校对共用） */
@Data
public class QuestionDTO {
    private Long id;
    private Integer qNo;
    /** SINGLE / MULTI / JUDGE / BLANK / SHORT */
    private String qType;
    private String stem;
    private String optionsJson;
    private String answerJson;
    private String analysis;
    private BigDecimal fullScore;
    private String groupName;
    private Integer sortNo;
    /**
     * 填空题的空数（学生端据此生成输入框）。
     * <p>取值顺序：老师给的标准答案条数 → 题干里连续下划线的组数 → 1；不含答案内容，可安全下发学生端。</p>
     */
    private Integer blankCount;
}
