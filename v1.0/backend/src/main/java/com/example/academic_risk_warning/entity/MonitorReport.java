package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学情监测报告表
 */
@Data
@TableName("monitor_report")
public class MonitorReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生ID */
    private Long studentId;

    /** 课程ID */
    private Long courseId;

    /** 报告日期 */
    private LocalDate reportDate;

    /** LLM生成的监测摘要 */
    private String summary;

    /** 六维风险分详情（JSON） */
    private String riskDimensions;

    /** 异常标记（JSON） */
    private String anomalyFlags;

    /** 创建时间 */
    private LocalDateTime createTime;
}
