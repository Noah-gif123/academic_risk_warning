package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生三维画像表
 */
@Data
@TableName("student_profile")
public class StudentProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生ID */
    private Long studentId;

    /** 知识点掌握度画像（JSON） */
    private String knowledgeProfile;

    /** 学习习惯画像（JSON） */
    private String habitProfile;

    /** 学习目标画像（JSON） */
    private String goalProfile;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
