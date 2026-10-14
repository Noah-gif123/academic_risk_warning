package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生自主设定的学习目标
 */
@Data
@TableName("student_goal")
public class StudentGoal {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生ID */
    private Long studentId;

    /** 课程ID */
    private Long courseId;

    /** 目标等级：冲刺/稳定/追赶/保底 */
    private String goalLevel;

    /** 目标分数 */
    private Integer targetScore;

    /** 里程碑列表（JSON数组） */
    private String milestones;

    /** 学生备注 */
    private String note;

    /** 截止日期 */
    private LocalDateTime deadline;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
