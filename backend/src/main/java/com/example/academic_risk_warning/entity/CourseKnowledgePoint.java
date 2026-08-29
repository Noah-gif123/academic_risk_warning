package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_knowledge_point")
public class CourseKnowledgePoint {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long parentId;
    private String name;
    private Integer sortOrder;
    private String description;
    private LocalDateTime createTime;
}
