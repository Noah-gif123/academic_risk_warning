package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 成绩构成视图：方案 + 评分项树 + 确认与锁定状态 */
@Data
public class ScoreSchemeVO {
    private Long id;
    private Long groupId;
    private Long offeringId;
    private Long termId;
    private String termName;
    private String courseName;
    private String courseType;
    private Integer version;
    private String level;
    private Long sourceTemplateId;
    private BigDecimal totalWeight;
    private String confirmStatus;
    private Integer confirmTotal;
    private Integer confirmDone;
    private Integer locked;
    private String lockType;
    private String lockedByName;
    private LocalDateTime lockedAt;
    private String remark;
    private Integer teacherCount;
    private Boolean myConfirmed;
    private List<Long> confirmedTeacherIds;
    private List<ScoreItemVO> items;
}
