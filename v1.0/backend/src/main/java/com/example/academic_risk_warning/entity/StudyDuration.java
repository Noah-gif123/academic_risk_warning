package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习时长记录表
 */
@Data
@TableName("study_duration")
public class StudyDuration {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    /** 教学周次(1-18) */
    private Integer weekNumber;

    /** 总学习时长(分钟) */
    private Integer totalMinutes;

    /** 线上学习时长 */
    private Integer onlineMinutes;

    /** 线下自习时长 */
    private Integer offlineMinutes;

    /** 练习做题时长 */
    private Integer exerciseMinutes;

    /** 讨论互动时长 */
    private Integer discussionMinutes;

    /** 最近登录时间 */
    private LocalDateTime lastLoginTime;

    /** 记录日期 */
    private LocalDate recordDate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
