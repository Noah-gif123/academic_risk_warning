package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.util.List;

/** 教师首页课程卡片（按「课程 × 学期」聚合成一张卡） */
@Data
public class TeacherCourseCardVO {
    private Long offeringId;
    private Long termId;
    private String termName;
    private String courseName;
    private String courseType;
    private Integer classCount;
    private Integer studentCount;
    private Integer closedClassCount;
    private List<Lite> classes;
    private Boolean allClosed;

    @Data
    public static class Lite {
        private Long teachingClassId;
        private String classCode;
        private String className;
        private Integer studentCount;
        private Integer effectiveSessions;
        private String status;
    }
}
