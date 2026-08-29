package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 知识点掌握情况表
 */
@Data
@TableName("knowledge_mastery")
public class KnowledgeMastery {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    private Integer totalKnowledge;

    private Integer errorCount;

    private Integer totalQuestion;

    private Integer weakKnowledgeCount;

    private Integer basicTotal;

    private Integer basicCorrect;
}
