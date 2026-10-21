package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 覆盖率体检记录表（去重与督办跟踪）
 * <p>对应表：coverage_check_log</p>
 */
@Data
@TableName("coverage_check_log")
public class CoverageCheckLog {

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

    /** 体检类型：SOFT期内软提醒/MID期中正式/FINAL期末正式 */
    private String checkType;

    /** 体检日期 */
    private LocalDate checkDate;

    /** 体检时教学周次 */
    private Integer weekNo;

    /** 应完成课次（分母：已过日期且未取消） */
    private Integer dueSessions;

    /** 已点名课次（分子） */
    private Integer recordedSessions;

    /** 点名覆盖率(%) */
    private BigDecimal coverageRate;

    /** 是否低于阈值：1是（进督办列表） */
    private Integer belowThreshold;

    /** 是否已提醒（老师+教务） */
    private Integer notified;

    /** 关联提醒通知ID */
    private Long notificationId;

    /** 是否已补录解决 */
    private Integer resolved;

    /** 解决时间 */
    private LocalDateTime resolvedAt;

    /** 督办说明 */
    private String resolveNote;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
