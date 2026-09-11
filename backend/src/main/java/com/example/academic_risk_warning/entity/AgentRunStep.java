package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体运行的单步明细（哪个智能体、成功与否、耗时、输出摘要、错误）
 */
@Data
@TableName("agent_run_step")
public class AgentRunStep {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long runId;

    private Integer stepNo;

    private String agentName;

    /** SUCCESS / FAILED */
    private String status;

    private Long durationMs;

    /** 产出摘要（截断后的 JSON） */
    private String outputDigest;

    private String error;

    /** 输出校验结果：PASS / WARN / FAIL / SKIP */
    private String validationStatus;

    /** 校验说明（结构/取值/真值一致性问题） */
    private String validationDetail;

    /** 反思信息（自检批评，触发 Self-Refine 时写入） */
    private String reflection;

    /** LLM 调用轮次（>1 表示触发了反思重写） */
    private Integer attempts;

    /** 产出顶层字段清单（逗号分隔，用于 golden set 精确核对） */
    private String outputFields;

    private LocalDateTime createTime;
}
