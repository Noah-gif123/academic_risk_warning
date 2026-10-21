package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 行政班挂科率表（→导员）
 * <p>对应表：admin_class_fail_rate</p>
 */
@Data
@TableName("admin_class_fail_rate")
public class AdminClassFailRate {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 行政班ID */
    private Long adminClassId;

    /** 开课ID */
    private Long offeringId;

    /** 课程ID */
    private Long courseId;

    /** 学期ID */
    private Long termId;

    /** 分母：实际考核人数 */
    private Integer examineeCount;

    /** 分子：挂科人数 */
    private Integer failCount;

    /** 挂科率(%) */
    private BigDecimal failRate;

    /** 其中硬门槛挂科人数 */
    private Integer hardFailCount;

    private String engineVersion;

    private LocalDateTime calcTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
