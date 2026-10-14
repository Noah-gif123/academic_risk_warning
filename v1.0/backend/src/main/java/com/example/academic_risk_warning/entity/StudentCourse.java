package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 学生-课程关联表
 */
@Data
@TableName("student_course")
public class StudentCourse {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;
}
