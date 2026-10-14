package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习分析报告表
 */
@Data
@TableName("analysis_report")
public class AnalysisReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生ID */
    private Long studentId;

    /** 课程ID */
    private Long courseId;

    /** 分析时间 */
    private LocalDateTime analysisDate;

    /** 薄弱点类别 */
    private String weaknessCategory;

    /** LLM生成的根源分析 */
    private String rootCause;

    /** 补强优先级排序（JSON） */
    private String prioritySequence;

    /** 创建时间 */
    private LocalDateTime createTime;
}
