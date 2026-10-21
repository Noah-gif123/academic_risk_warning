package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 课程挂科率历史表（→课程因子 c）
 * <p>对应表：course_fail_rate_history</p>
 */
@Data
@TableName("course_fail_rate_history")
public class CourseFailRateHistory {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 课程ID */
    private Long courseId;

    /** 学期ID */
    private Long termId;

    /** 分母：该课程本学期合计实际考核人数（按人数加权） */
    private Integer examineeCount;

    /** 分子：合计挂科人数 */
    private Integer failCount;

    /** 课程挂科率(%)（入历史，供下学期课程因子 c 标定） */
    private BigDecimal failRate;

    private Integer hardFailCount;

    /** 本学期该课程教学班数 */
    private Integer teachingClassCount;

    /** 是否手工初始化值（历史首值先手填，后续自动更新） */
    private Integer isInitialized;

    /** 由本挂科率映射出的课程因子 c（映射函数由引擎定义） */
    private BigDecimal courseFactor;

    private String engineVersion;

    private LocalDateTime calcTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
