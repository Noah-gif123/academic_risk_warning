package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 教学班表（1 位老师）
 * <p>对应表：teaching_class</p>
 */
@Data
@TableName("teaching_class")
public class TeachingClass {

    /** 教学班ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 开课ID */
    private Long offeringId;

    /** 任课教师ID（一个教学班只有一位老师） */
    private Long teacherId;

    /** 教学班编码，如 CS101-01 */
    private String classCode;

    /** 教学班名称，如 计算机基础2-01班 */
    private String className;

    /** 容量（可选） */
    private Integer capacity;

    /** 是否重修班：1是（仅展示与筛选用，统计上不作特殊处理） */
    private Integer isRetakeClass;

    /** 6 位入班邀请码（教师生成，学期内有效、次数不限） */
    private String inviteCode;

    /** 是否开启入班审核：1需要教师同意 */
    private Integer joinReviewEnabled;

    /** 状态：ACTIVE/CLOSED */
    private String status;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
