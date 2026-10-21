package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 逐题作答与判分表
 * <p>对应表：submission_answer</p>
 */
@Data
@TableName("submission_answer")
public class SubmissionAnswer {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 提交记录ID */
    private Long submissionId;

    /** 题目ID */
    private Long questionId;

    /** 学生答案（结构同 question.answer_json） */
    private String answerJson;

    /** 本题得分 */
    private BigDecimal score;

    /** 客观题是否正确：1对/0错（主观题为空） */
    private Integer isCorrect;

    /** 教师批注 */
    private String comment;

    /** 判卷人类型：SYSTEM系统/TEACHER教师/AI智能 */
    private String markedByType;

    /** 判卷人ID（教师ID；系统或AI为空） */
    private Long markedBy;

    /** 判卷时间 */
    private LocalDateTime markedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
