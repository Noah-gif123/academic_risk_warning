package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 作业完成情况表
 */
@Data
@TableName("homework_info")
public class HomeworkInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    private Integer totalHomework;

    private Integer submitCount;

    private Integer notSubmitCount;

    private Integer lateSubmitCount;

    private String scoreList;

    private Integer avgScore;
}
