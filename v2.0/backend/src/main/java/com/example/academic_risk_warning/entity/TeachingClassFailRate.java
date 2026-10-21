package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 教学班挂科率表（→老师）
 * <p>对应表：teaching_class_fail_rate</p>
 */
@Data
@TableName("teaching_class_fail_rate")
public class TeachingClassFailRate {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 教学班ID */
    private Long teachingClassId;

    /** 开课ID */
    private Long offeringId;

    /** 任课教师ID */
    private Long teacherId;

    /** 学期ID */
    private Long termId;

    /** 分母：实际考核人数（教务二次确认，扣除缓考/无成绩） */
    private Integer examineeCount;

    /** 分子：挂科人数 */
    private Integer failCount;

    /** 挂科率(%) = fail_count / examinee_count × 100 */
    private BigDecimal failRate;

    /** 其中因 1/3 硬门槛挂科的人数 */
    private Integer hardFailCount;

    /** 是否重修班（仅展示；不作特殊排除） */
    private Integer isRetakeClass;

    private String engineVersion;

    /** 计算时间 */
    private LocalDateTime calcTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
