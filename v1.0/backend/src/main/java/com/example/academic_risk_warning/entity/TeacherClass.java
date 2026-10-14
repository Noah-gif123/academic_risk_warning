package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师负责班级表（与 student.class_name 关联）
 */
@Data
@TableName("teacher_class")
public class TeacherClass {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;

    /** 班级名称，如：计算机1班（大一） */
    private String className;

    /** 分配给该教师的学生ID起始值（闭区间） */
    private Long studentIdStart;

    /** 分配给该教师的学生ID结束值（闭区间） */
    private Long studentIdEnd;

    private LocalDateTime createTime;
}
