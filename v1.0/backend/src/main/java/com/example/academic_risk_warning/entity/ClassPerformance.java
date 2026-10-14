package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 课堂表现表
 */
@Data
@TableName("class_performance")
public class ClassPerformance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    private Integer totalClassTimes;

    private Integer absentCount;

    private Integer lateCount;

    private Integer quizScore;

    private String qaEvaluate;

    private String attitudeEvaluate;
}
