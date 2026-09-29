package com.example.jyh1013.entiy;

import lombok.Data;

/**
 * 学生课程关联实体（对应student_course_rel表）
 */
@Data  // Lombok自动生成getter/setter/toString等方法
public class CourseSelection {
    private String studentNo;     // 学生编号（对应student_no）
    private String courseNo;      // 课程编号（对应course_no）
    private String termNo;        // 学期编号（对应term_no）
    private String teachingClassNo; // 教学班编号（对应teaching_class_no）
    private String teacherId;     // 教师编号（对应teacher_id）
}
