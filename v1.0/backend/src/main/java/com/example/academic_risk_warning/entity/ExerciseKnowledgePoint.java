package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("exercise_knowledge_point")
public class ExerciseKnowledgePoint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long exerciseId;
    private Long kpId;
}
