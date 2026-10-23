package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 预警记录表（重建：带通道/置信度/引擎版本）
 * <p>对应表：alert_record</p>
 */
@Data
@TableName("alert_record")
public class AlertRecord {

    /** 预警ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 学生ID */
    private Long studentId;

    /** 选课ID */
    private Long enrollmentId;

    /** 开课ID */
    private Long offeringId;

    /** 教学班ID */
    private Long teachingClassId;

    /** 课程ID */
    private Long courseId;

    /** 学期ID */
    private Long termId;

    /** 预警等级：RED/ORANGE/YELLOW */
    private String alertLevel;

    /** 触发通道：A确定性/B班内相对/C跨班可比 */
    private String triggerChannel;

    /** 触发规则：A_HARD_LINE已超1/3/A_NEAR_LINE剩一次/A_UNREACHABLE期末不可达/B_RANK班内靠前/C_THRESHOLD跨班超标 */
    private String triggerRule;

    /** 触发时风险分 */
    private BigDecimal riskScore;

    /** 触发时置信度 */
    private BigDecimal confidence;

    /** 触发时输出形态：FULL_SCORE/SCORE_WITH_NOTE/BEHAVIOR_ONLY/INSUFFICIENT */
    private String outputMode;

    /** 人话版预警原因（面向老师/学生） */
    private String alertReason;

    /** 触发依据快照：{"absent":9,"allowed":10,"remaining":1,"needFinal":93.8,"coverage":0.72} */
    private String evidenceJson;

    /** 触发时三通道值快照：{"a":"DANGER","b":68.2,"c":null} */
    private String channelSnapshotJson;

    /** 状态：ACTIVE处理中/ACKNOWLEDGED已知悉/HANDLING处理中/CLOSED已关闭/DISMISSED已忽略/EXPIRED超时关闭 */
    private String status;

    /** 是否当前生效预警：1当前 / NULL历史（配合唯一键保证每生每课仅一条当前预警） */
    private Integer isCurrent;

    /** 是否已升级过等级 */
    private Integer escalated;

    /** 升级时间 */
    private LocalDateTime escalatedAt;

    /** 升级前等级 */
    private String previousLevel;

    /** 是否已发出通知 */
    private Integer notified;

    /** 首次通知时间 */
    private LocalDateTime firstNotifyTime;

    /** 超时时间（超时未处理 → 自动升级/关闭） */
    private LocalDateTime expireTime;

    /** 处理人ID */
    private Long handleBy;

    /** 处理人角色：TEACHER/COUNSELOR/ADMIN */
    private String handleRole;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理说明 */
    private String handleNote;

    /** 关闭人ID */
    private Long closeBy;

    /** 关闭时间 */
    private LocalDateTime closeTime;

    /** 关闭说明/结论 */
    private String closeNote;

    /** 忽略原因 */
    private String dismissReason;

    /** 学生回应（学生端可回执） */
    private String studentResponse;

    /** 学生回应时间 */
    private LocalDateTime studentResponseTime;

    /** 教师对申诉的回复（学生端可见） */
    private String teacherReply;

    /** 教师回复时间 */
    private LocalDateTime teacherReplyTime;

    /** 产生该预警的引擎版本 */
    private String engineVersion;

    /** 是否作废（退课/重算）：1作废，不进任何统计与列表 */
    private Integer isVoid;

    private String voidReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
