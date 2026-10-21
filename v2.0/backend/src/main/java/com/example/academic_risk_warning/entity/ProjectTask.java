package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 项目任务表
 * <p>对应表：project_task</p>
 */
@Data
@TableName("project_task")
public class ProjectTask {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 教学班ID */
    private Long teachingClassId;

    /** 开课ID（冗余） */
    private Long offeringId;

    /** 学期ID */
    private Long termId;

    /** 项目名称 */
    private String projectName;

    /** 要求说明 */
    private String description;

    /** 所属课时ID（可空） */
    private Long lessonId;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 截止时间（逾期不允许补交） */
    private LocalDateTime endTime;

    /** 是否团队项目：1是（需组长提交贡献表） */
    private Integer isTeam;

    /** 项目满分 */
    private BigDecimal fullScore;

    /** 允许的提交形式（逗号分隔） */
    private String allowFormats;

    /** 计入的评分项ID（为空表示仅记录不计分） */
    private Long schemeItemId;

    /** 发起人（教师ID） */
    private Long publisherId;

    /** 状态：DRAFT草稿/PUBLISHED已发布/CLOSED已截止 */
    private String status;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
