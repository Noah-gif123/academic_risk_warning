package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 学生端我的成绩（分项 + 总评；不给 Δ 等教务专属口径） */
@Data
public class StudentScoreVO {
    private Long teachingClassId;
    private String courseName;
    private String teacherName;
    private String termName;
    private Integer passScore;
    private BigDecimal weightedScore;
    private BigDecimal deductionPoints;
    private BigDecimal totalScore;
    private Integer absentCount;
    private Integer lateCount;
    private Integer leaveCount;
    private Integer allowedAbsent;
    private Boolean hardFail;
    private Boolean fail;
    private String failReasonText;
    private String scoreStatus;
    private String scoreStatusText;
    private List<Item> items;

    @Data
    public static class Item {
        private String itemName;
        private String path;
        private BigDecimal weight;
        private BigDecimal score;
        private BigDecimal weightPercent;
        private Boolean missing;
    }
}
