package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 教师尺度Δ表（分项级，含收缩与熔断）
 * <p>对应表：teacher_style</p>
 */
@Data
@TableName("teacher_style")
public class TeacherStyle {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 教师ID */
    private Long teacherId;

    /** 开课ID（Δ 是"该教师在该课"的尺度） */
    private Long offeringId;

    /** 课程ID */
    private Long courseId;

    /** 学期ID */
    private Long termId;

    /** 评分项ID（分项级 Δ；整体级为空） */
    private Long schemeItemId;

    /** 评分项编码；整体级固定 OVERALL */
    private String itemCode;

    /** 未收缩的原始 Δ（班 vs 课程平均） */
    private BigDecimal rawDelta;

    /** 收缩后的 Δ̂ = raw_delta × λ（实际用于折算） */
    private BigDecimal delta;

    /** 收缩系数 λ = n/(n+k)，k 见 risk_engine_config */
    private BigDecimal shrinkLambda;

    /** 参与估计的样本量（学生数） */
    private Integer sampleSize;

    /** 样本标准差（异常检测用） */
    private BigDecimal stdDev;

    /** 是否熔断：1停用该 Δ（|Δ| 超阈值，等教务复核） */
    private Integer fused;

    /** 熔断原因/复核说明 */
    private String fuseReason;

    /** 复核人（教务ID） */
    private Long reviewBy;

    /** 复核时间 */
    private LocalDateTime reviewTime;

    /** 状态：ACTIVE可用/INSUFFICIENT样本不足/FUSED已熔断/REVIEWED复核通过 */
    private String status;

    private String engineVersion;

    /** 估计时间 */
    private LocalDateTime calcTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
