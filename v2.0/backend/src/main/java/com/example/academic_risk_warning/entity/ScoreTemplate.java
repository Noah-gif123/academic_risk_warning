package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 成绩构成模板表
 * <p>对应表：score_template</p>
 */
@Data
@TableName("score_template")
public class ScoreTemplate {

    /** 模板ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 模板编码，如 EXAM_DEFAULT */
    private String templateCode;

    /** 模板名称，如 考试课默认构成 */
    private String templateName;

    /** 适用课程类型：EXAM/ASSESS/EXPERIMENT/PROJECT */
    private String courseType;

    /** 适用层级：L1/L2/L3 */
    private String level;

    /** 评分项定义（两级，结构见上方注释） */
    private String itemsJson;

    /** 说明 */
    private String description;

    /** 是否该课程类型的默认模板 */
    private Integer isDefault;

    private String status;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
