package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生作答草稿
 * 用于自动保存学生作答过程中的草稿内容
 */
@Data
@TableName("exercise_draft")
public class ExerciseDraft {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recommendId;
    private Long studentId;
    private String answer;          // 无子题时的作答文本
    private String subAnswers;      // JSON格式的子题作答 Map<subQuestionId, answer>
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
