package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exercise_sub_question")
public class ExerciseSubQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long exerciseId;
    private Integer seq;
    private String title;
    private Long kpId;
    private String questionType;    // SINGLE=单知识点 / COMPREHENSIVE=综合多知识点
    private String knowledgePoint;
    private Integer score;
    private String referenceAnswer;
    private LocalDateTime createTime;
}
