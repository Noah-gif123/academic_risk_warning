package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 干预记录表（重构：支持教师/导员/教务三级与上报闭环）
 * <p>对应表：intervention_record</p>
 */
@Data
@TableName("intervention_record")
public class InterventionRecord {

    /** 干预记录ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 学生ID */
    private Long studentId;

    /** 学期ID（旧数据迁移时可为空） */
    private Long termId;

    /** 关联预警ID（可空：可由教师主动发起，不只针对单条预警） */
    private Long alertId;

    /** 关联预警ID数组（一次处理可覆盖多条预警） */
    private String relatedAlertIds;

    /** 阶段：TEACHER教师处理/COUNSELOR_1导员第1次/COUNSELOR_2导员第2次/REFERRED已上报教务/ADMIN_TALK教务面谈/ADMIN_HELP教务具体帮助/TERMINATED终止干预 */
    private String stage;

    /** 处理人ID */
    private Long operatorId;

    /** 处理人角色：TEACHER/COUNSELOR/ADMIN */
    private String operatorRole;

    /** 动作：TALK谈话/CLASS_MEETING班会/PARENT联系家长/SUPPLEMENT补课补习/PLAN学习计划/INTERVIEW面谈/HELP具体帮助/OTHER其他 */
    private String actionType;

    /** 处理内容 */
    private String content;

    /** 结果描述 */
    private String result;

    /** 是否有成效：EFFECTIVE有效/INEFFECTIVE无效/PENDING待观察 */
    private String effective;

    /** 成效检查时间（= 下次 risk_state 刷新 或 干预后 14 天，先到为准） */
    private LocalDateTime effectCheckTime;

    /** 干预时风险分（快照） */
    private BigDecimal riskScoreBefore;

    /** 成效检查时的风险分 */
    private BigDecimal riskScoreAfter;

    /** 风险分变化（负数=改善） */
    private BigDecimal riskScoreChange;

    /** 上报状态：NONE未上报/REFERRED已上报/ADMIN_HANDLING教务处理中/ADMIN_DONE教务已完结 */
    private String referStatus;

    /** 上报人（导员ID） */
    private Long referBy;

    /** 上报时间 */
    private LocalDateTime referTime;

    /** 上报说明（导员判定"收集齐全"的说明） */
    private String referNote;

    /** 教务处理反馈（导员可追踪"交上去之后怎么样了"） */
    private String adminFeedback;

    /** 是否作废 */
    private Integer isVoid;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
