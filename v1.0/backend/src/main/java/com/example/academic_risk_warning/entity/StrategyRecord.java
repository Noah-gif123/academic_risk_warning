package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 策略调整记录表
 */
@Data
@TableName("strategy_record")
public class StrategyRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生ID */
    private Long studentId;

    /** 课程ID */
    private Long courseId;

    /** 上一策略描述 */
    private String previousStrategy;

    /** 新策略描述 */
    private String newStrategy;

    /** 调整原因 */
    private String changeReason;

    /** 触发来源: FEEDBACK / EFFECT_CHECK / MANUAL */
    private String triggerSource;

    /** 创建时间 */
    private LocalDateTime createTime;
}
