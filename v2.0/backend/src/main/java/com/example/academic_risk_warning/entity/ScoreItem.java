package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 分项得分明细表（分项级 Δ 的数据基础）
 * <p>对应表：score_item</p>
 */
@Data
@TableName("score_item")
public class ScoreItem {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 选课ID */
    private Long enrollmentId;

    /** 学生ID（由 enrollment 决定，写入时校验一致） */
    private Long studentId;

    /** 评分项ID（score_scheme_item） */
    private Long schemeItemId;

    /** 学期ID */
    private Long termId;

    /** 第几次（作业第几次、实验第几次；单次项恒为 1） */
    private Integer timesNo;

    /** 原始取值（等级制/通过制用，如 优秀/通过/不通过） */
    private String rawValue;

    /** 得分（等级制已按 mapping_json 换算为数值） */
    private BigDecimal score;

    /** 该项满分（写入时快照，防止构成改动影响历史） */
    private BigDecimal fullScore;

    /** 是否缺交/缺考：1是（区别于"尚未录入"；统计时按 0 分计） */
    private Integer isAbsent;

    /** 来源：MANUAL老师录入/AUTO系统自动算（如作业均分） */
    private String source;

    /** 打分人ID */
    private Long scorerId;

    /** 打分人角色：TEACHER/ADMIN/SYSTEM */
    private String scorerRole;

    /** 打分时间 */
    private LocalDateTime scoredAt;

    /** 是否作废 */
    private Integer isVoid;

    private String voidReason;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
