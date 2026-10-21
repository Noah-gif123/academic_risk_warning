package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 评分项视图（树） */
@Data
public class ScoreItemVO {
    private Long id;
    private Long parentId;
    private Integer itemLevel;
    private String itemCode;
    private String itemName;
    private String category;
    private BigDecimal weight;
    private BigDecimal fullScore;
    private String dataSource;
    private String scoreType;
    private String aggregateMode;
    private Integer expectedTimes;
    private Integer isAnchor;
    private String anchorLayer;
    private Integer isSubjective;
    private String scorerRole;
    private Integer sortNo;
    private List<ScoreItemVO> children;
}
