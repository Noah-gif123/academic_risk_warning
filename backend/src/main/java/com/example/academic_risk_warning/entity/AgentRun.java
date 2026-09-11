package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一次智能体流水线运行记录（教师端"运行历史"用）
 */
@Data
@TableName("agent_run")
public class AgentRun {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    /** FULL / FEEDBACK / EFFECT_BASED 等 */
    private String pipeline;

    /** MANUAL / SCHEDULED / ALERT_TRIGGERED / FEEDBACK */
    private String triggerType;

    /** RUNNING / SUCCESS / PARTIAL / FAILED */
    private String status;

    private Integer totalSteps;

    private Integer succeededSteps;

    private Integer failedSteps;

    /** 各步耗时之和（毫秒） */
    private Long totalMs;

    private String error;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;
}
