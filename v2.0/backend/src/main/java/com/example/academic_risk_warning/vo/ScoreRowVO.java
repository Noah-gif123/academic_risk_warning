package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 矩阵一行 = 一个学生 */
@Data
public class ScoreRowVO {
    private Long enrollmentId;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Map<Long, ScoreCellVO> cells;
    private BigDecimal weightedScore;
    private BigDecimal deductionPoints;
    private BigDecimal totalScore;
    private Integer absentCount;
    private Integer lateCount;
    private Integer leaveCount;
    private Integer allowedAbsent;
    private Integer effectiveSessions;
    private Boolean hardFail;
    private Boolean fail;
    private String failReason;
    private String scoreStatus;
    /** 还没录成绩的评分项名称 */
    private List<String> missingItems;
}
