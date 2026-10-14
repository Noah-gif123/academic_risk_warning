package com.example.academic_risk_warning.vo;

import lombok.Data;

/**
 * 教师关联课程概览
 */
@Data
public class TeacherCourseVO {

    private Long courseId;

    private String courseName;

    /** 该课程下教师负责的学生数 */
    private Integer studentCount;

    /** 该课程 ACTIVE 状态的预警数 */
    private Integer alertCount;
}
