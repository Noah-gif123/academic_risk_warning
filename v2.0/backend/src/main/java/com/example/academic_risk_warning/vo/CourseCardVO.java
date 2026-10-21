package com.example.academic_risk_warning.vo;

import lombok.Data;

/** 学生端课程卡片 */
@Data
public class CourseCardVO {
    private Long teachingClassId;
    private Long offeringId;
    private String courseName;
    private String courseType;
    private String className;
    private String teacherName;
    private String termName;
    private Long termId;
    private String status;
    private String statusText;
    private Boolean canEnter;
    private String enrollmentStatus;
}
