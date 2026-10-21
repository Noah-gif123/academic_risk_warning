package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 评分项（两级）：children 为空表示叶子项 */
@Data
public class ScoreItemDTO {
    private String itemCode;
    private String itemName;
    /** 分类：USUAL/MID/FINAL/HOMEWORK/QUIZ/PROJECT/EXP/PERF/OTHER（禁止 ATTENDANCE） */
    private String category;
    private BigDecimal weight;
    private BigDecimal fullScore;
    /** 数据来源：AUTO 自动汇总（测验/作业/项目）/ MANUAL 手工录入 */
    private String dataSource;
    /** 记分形态：NUMERIC / PASS / GRADE */
    private String scoreType;
    private String aggregateMode;
    private Integer expectedTimes;
    private Integer isAnchor;
    private String anchorLayer;
    private Integer isSubjective;
    private String scorerRole;
    private List<ScoreItemDTO> children;
}
