package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exercise_recommendation")
public class ExerciseRecommendation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long exerciseId;
    private Long studentId;
    private Long alertId;
    private String knowledgePoint;
    private String status;
    private Integer score;
    private String studentAnswer;
    private String teacherFeedback;
    private LocalDateTime recommendTime;
    private LocalDateTime completeTime;

    /** AI推荐理由 */
    private String aiReason;

    /** 推荐优先级 */
    private Integer priority;

    /** 推荐类型: EXERCISE / VIDEO / READING */
    private String recommendType;
}
