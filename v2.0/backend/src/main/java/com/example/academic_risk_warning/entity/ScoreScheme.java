package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 成绩构成表（课程组共用，锁定后不可改）
 * <p>对应表：score_scheme</p>
 */
@Data
@TableName("score_scheme")
public class ScoreScheme {

    /** 构成ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 课程组ID */
    private Long groupId;

    /** 学期ID（业务表统一带） */
    private Long termId;

    /** 版本号（同一课程组内自增，最新版为当前生效版） */
    private Integer version;

    /** 锁定时的层级快照（L1/L2/L3） */
    private String level;

    /** 来源模板ID（可空） */
    private Long sourceTemplateId;

    /** 一级项权重合计（锁定前必须=100.00） */
    private BigDecimal totalWeight;

    /** 确认状态：DRAFT草稿/CONFIRMING确认中/CONFIRMED已确认/LOCKED已锁定 */
    private String confirmStatus;

    /** 应确认人数（该课程组本学期任课老师数） */
    private Integer confirmTotal;

    /** 已确认人数 */
    private Integer confirmDone;

    /** 发起人（教师ID） */
    private Long initiatorId;

    /** 确认明细：[{"teacherId":1,"teacherName":"李老师","time":"..."}] */
    private String confirmedByJson;

    /** 是否已锁定：1已锁（学期内不可改） */
    private Integer locked;

    /** 锁定操作人 */
    private Long lockedBy;

    /** 锁定时间 */
    private LocalDateTime lockedAt;

    /** 锁定方式：TEACHER_COLLECTIVE全体确认/ADMIN_RULED教务裁定/TEMPLATE_AUTO模板默认 */
    private String lockType;

    /** 备注（如教务裁定原因） */
    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
