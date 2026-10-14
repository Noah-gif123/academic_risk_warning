package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预警历史快照表
 */
@Data
@TableName("alert_snapshot")
public class AlertSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    /** 快照日期 */
    private LocalDate snapshotDate;

    /** 快照周次 */
    private Integer snapshotWeek;

    /** 预警等级: RED/ORANGE/YELLOW/GREEN */
    private String alertLevel;

    /** 综合风险分 */
    private BigDecimal riskScore;

    private BigDecimal academicRiskScore;

    private BigDecimal homeworkRiskScore;

    private BigDecimal attendanceRiskScore;

    private BigDecimal knowledgeRiskScore;

    private BigDecimal historyRiskScore;

    /** 学习时长风险分 */
    private BigDecimal studyDurationRiskScore;

    /** 预测成绩 */
    private BigDecimal predictedScore;

    /** 触发预警类型(逗号分隔) */
    private String alertTypes;

    /** 平时成绩 */
    private Integer usualScore;

    /** 期中成绩 */
    private Integer midScore;

    /** 期末成绩 */
    private Integer finalScore;

    /** 作业提交率% */
    private BigDecimal homeworkSubmitRate;

    /** 出勤率% */
    private BigDecimal attendanceRate;

    /** 知识点正确率% */
    private BigDecimal knowledgeCorrectRate;

    /** 近4周平均学习时长 */
    private Integer studyTotalMinutes;

    /** 是否触发预警 */
    private Boolean isGeneratedAlert;

    /** 关联预警ID */
    private Long relatedAlertId;

    private LocalDateTime createTime;
}
