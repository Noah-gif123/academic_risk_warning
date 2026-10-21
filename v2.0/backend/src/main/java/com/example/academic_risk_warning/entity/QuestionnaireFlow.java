package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 问卷流转状态表（防 4 环节断链）
 * <p>对应表：questionnaire_flow</p>
 */
@Data
@TableName("questionnaire_flow")
public class QuestionnaireFlow {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 问卷ID */
    private Long questionnaireId;

    /** 学期ID */
    private Long termId;

    /** 环节：COUNSELOR_COLLECT导员收集/ADMIN_RECEIVE教务接收/ADMIN_DISTRIBUTE教务分发/TEACHER_VIEW授课组查看 */
    private String stage;

    /** 环节顺序（1–4） */
    private Integer stageOrder;

    /** 当前责任人ID */
    private Long holderId;

    /** 当前责任人角色：COUNSELOR/ADMIN/TEACHER */
    private String holderRole;

    /** 进入本环节时间 */
    private LocalDateTime enteredAt;

    /** 本环节截止时间 */
    private LocalDateTime deadline;

    /** 状态：PENDING未开始/IN_PROGRESS进行中/DONE已完成/OVERDUE已超时 */
    private String status;

    /** 完成时间 */
    private LocalDateTime doneAt;

    /** 说明 */
    private String note;

    /** 催办次数 */
    private Integer remindCount;

    /** 最近催办时间 */
    private LocalDateTime lastRemindTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
