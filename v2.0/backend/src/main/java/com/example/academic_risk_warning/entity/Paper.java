package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 试卷表（测验/作业/考试共用）
 * <p>对应表：paper</p>
 */
@Data
@TableName("paper")
public class Paper {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 教学班ID */
    private Long teachingClassId;

    /** 开课ID（冗余） */
    private Long offeringId;

    /** 学期ID */
    private Long termId;

    /** 类型：QUIZ随堂测验/HOMEWORK课后作业/EXAM线上考试 */
    private String paperType;

    /** 试卷名称（学生端卡片标题的一部分） */
    private String paperName;

    /** 所属课时ID（测验/作业挂课用；考试为空） */
    private Long lessonId;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间（到点自动交卷、逾期不允许补交） */
    private LocalDateTime endTime;

    /** 限时（分钟；为空表示不限时） */
    private Integer durationMinutes;

    /** 试卷满分（本次分值，非平时成绩） */
    private BigDecimal fullScore;

    /** 计入的评分项ID（score_scheme_item）；为空表示仅记录不计分 */
    private Long schemeItemId;

    /** 发起人角色：TEACHER教师/ADMIN教务 */
    private String publisherRole;

    /** 发起人ID */
    private Long publisherId;

    /** 考试/测验完成后是否允许学生查看试卷：1是 */
    private Integer allowViewPaper;

    /** 是否向学生显示成绩：1是 */
    private Integer showScore;

    /** 答案与解析可见性：NEVER永远不可见/AFTER_MARK批阅后可见 */
    private String showAnswer;

    /** 状态：DRAFT草稿/PUBLISHED已发布/CLOSED已截止 */
    private String status;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
