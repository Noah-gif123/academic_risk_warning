package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 学生跨学期成绩/挂科历史表
 * <p>对应表：student_fail_history</p>
 */
@Data
@TableName("student_fail_history")
public class StudentFailHistory {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 学生ID */
    private Long studentId;

    /** 学期ID */
    private Long termId;

    /** 课程ID */
    private Long courseId;

    /** 开课ID */
    private Long offeringId;

    /** 最终总评 T（快照） */
    private BigDecimal finalScore;

    /** 及格线（快照） */
    private Integer passScore;

    /** 是否挂科 */
    private Integer isFail;

    /** 是否因 1/3 硬门槛挂科 */
    private Integer hardFail;

    /** 是否重修记录 */
    private Integer isRetake;

    /** 成绩状态（FINAL/DEFERRED_GIVEUP/ABSENT_EXAM…） */
    private String scoreStatus;

    private String engineVersion;

    /** 归档为历史的时间（学期结束结算后写入） */
    private LocalDateTime archivedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
