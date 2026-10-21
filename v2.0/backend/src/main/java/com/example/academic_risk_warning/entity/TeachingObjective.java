package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 教学目标表
 * <p>对应表：teaching_objective</p>
 */
@Data
@TableName("teaching_objective")
public class TeachingObjective {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 教学班ID */
    private Long teachingClassId;

    /** 开课ID */
    private Long offeringId;

    /** 学期ID */
    private Long termId;

    /** 教学目标编号（教师自编，如 ADanaN4） */
    private String objCode;

    /** 教学目标内容 */
    private String objContent;

    /** 目标类型（教师自填，如 能力目标/素质目标） */
    private String objType;

    /** 展示顺序 */
    private Integer sortNo;

    /** 状态：ACTIVE/DISABLED */
    private String status;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
