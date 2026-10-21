package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 班集体提醒去重表（导员端，防天天提醒）
 * <p>对应表：class_alert_notification</p>
 */
@Data
@TableName("class_alert_notification")
public class ClassAlertNotification {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 行政班ID */
    private Long adminClassId;

    /** 学期ID */
    private Long termId;

    /** 主责导员ID */
    private Long counselorId;

    /** 档位：L1_MEETING建议开班会/L2_FOCUS重点关注班级 */
    private String ruleLevel;

    /** 触发时的教学周次（≥5 才可能触发） */
    private Integer weekNo;

    /** 该班学生数（分母） */
    private Integer classStudentCount;

    /** 预警学生数（分子，风险分 ≥ 阈值） */
    private Integer alertStudentCount;

    /** 预警占比(%) */
    private BigDecimal alertRate;

    /** 本次统计使用的风险分下限（默认 40） */
    private BigDecimal riskFloorUsed;

    /** 触发阈值快照：{"rate":0.30,"minCount":5,"weekStart":5} */
    private String thresholdJson;

    /** 纳入本次提醒的学生ID列表（可回溯"这条提醒包含谁"） */
    private String studentIdsJson;

    /** 生成的汇总通知ID */
    private Long notificationId;

    /** 是否抄送教务（档位2 为 1） */
    private Integer ccAdmin;

    /** 是否已发送 */
    private Integer isSent;

    private LocalDateTime sentTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
