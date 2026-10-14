package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sub_question_answer")
public class SubQuestionAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recommendId;
    private Long subQuestionId;
    private String studentAnswer;
    private Integer score;
    private String status;
    private LocalDateTime answerTime;
    private LocalDateTime gradeTime;
}
