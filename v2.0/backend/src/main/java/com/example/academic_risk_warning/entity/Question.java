package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 题目表
 * <p>对应表：question</p>
 */
@Data
@TableName("question")
public class Question {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 试卷ID */
    private Long paperId;

    /** 题号（试卷内序号） */
    private Integer qNo;

    /** 题型：SINGLE单选/MULTI多选/JUDGE判断/BLANK填空/SHORT简答 */
    private String qType;

    /** 题干 */
    private String stem;

    /** 选项（选择题使用） */
    private String optionsJson;

    /** 标准答案（结构见上） */
    private String answerJson;

    /** 解析（学生端是否可见取决于 paper.show_answer） */
    private String analysis;

    /** 本题分值 */
    private BigDecimal fullScore;

    /** 大题名称（如“第一大题 单选题”），用于学生端分组展示 */
    private String groupName;

    /** 排序 */
    private Integer sortNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
