package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 干预措施记录表
 */
@Data
@TableName("intervention_record")
public class InterventionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long alertId;

    private Long studentId;

    private Long teacherId;

    /** TALK/TUTOR/PARENT/SUPPLEMENT/PLAN/OTHER */
    private String interventionType;

    private String description;

    /** EXECUTING/COMPLETED/INEFFECTIVE */
    private String status;

    private String resultNote;

    /** 干预时风险分 */
    private BigDecimal riskScoreBefore;

    /** 干预后最新风险分 */
    private BigDecimal riskScoreAfter;

    /** 风险分变化(负数=改善) */
    private BigDecimal riskScoreChange;

    /** 效果检查时间 */
    private LocalDateTime effectCheckTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
