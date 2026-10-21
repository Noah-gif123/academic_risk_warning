package com.example.academic_risk_warning.vo;

import lombok.Data;

/** 学生端 / 教师端的课程卡片 */
@Data
public class CourseBriefVO {
    private Long offeringId;
    private String courseName;
    private String courseType;
    private Long teachingClassId;
    private String className;
    private String teacherName;
    private String termName;
    private Integer effectiveSessions;
    private String status;
    private Integer studentCount;
}
