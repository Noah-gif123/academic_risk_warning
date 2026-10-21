package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 问卷表（老师评价 / 知识点讲解反馈）
 * <p>对应表：questionnaire</p>
 */
@Data
@TableName("questionnaire")
public class Questionnaire {

    /** 问卷ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 开课ID */
    private Long offeringId;

    /** 课程ID */
    private Long courseId;

    /** 学期ID */
    private Long termId;

    /** 类型：TEACHER_EVAL老师评价（本版只此一种，用于教师风格判定） */
    private String questionnaireType;

    /** 问卷标题 */
    private String title;

    /** 题目定义：[{"code":"Q1","type":"SCORE","text":"..."}] */
    private String questionJson;

    /** 是否匿名（老师评价默认匿名：不存学生ID，只存去重令牌） */
    private Integer isAnonymous;

    /** 收集方式：COUNSELOR导员统一收集/ONLINE线上直收 */
    private String collectorType;

    /** 收集人（导员ID） */
    private Long collectorId;

    /** 收集覆盖的行政班ID数组 */
    private String collectorClassIdsJson;

    /** 开放时间 */
    private LocalDateTime openTime;

    /** 截止时间 */
    private LocalDateTime closeTime;

    /** 状态：DRAFT草稿/OPEN收集中/CLOSED已截止/SUBMITTED已交教务/DISTRIBUTED已分发/ARCHIVED已归档 */
    private String status;

    /** 已作答份数 */
    private Integer answerCount;

    /** 提交教务的人（导员ID） */
    private Long submitToAdminBy;

    /** 提交教务时间 */
    private LocalDateTime submitToAdminTime;

    /** 分发人（教务ID） */
    private Long distributedBy;

    /** 分发时间 */
    private LocalDateTime distributedTime;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
