package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 问卷作答明细表（匿名去重）
 * <p>对应表：questionnaire_answer</p>
 */
@Data
@TableName("questionnaire_answer")
public class QuestionnaireAnswer {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 问卷ID */
    private Long questionnaireId;

    /** 作答人去重令牌（匿名问卷用于防重复提交，不暴露身份） */
    private String respondentToken;

    /** 学生ID（匿名问卷为空） */
    private Long studentId;

    /** 选课ID（可空） */
    private Long enrollmentId;

    /** 被评价教师ID（老师评价类） */
    private Long targetTeacherId;

    /** 题目编码 */
    private String itemCode;

    /** 作答类型：SCORE评分/TEXT文本（本版只此两种） */
    private String answerType;

    /** 评分值（如 1–5 分） */
    private BigDecimal scoreValue;

    /** 文本作答 */
    private String textValue;

    /** 提交时间 */
    private LocalDateTime submitTime;

    private LocalDateTime createTime;
}
