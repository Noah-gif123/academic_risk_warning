package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 学生端课程详情：六个页签的数据一次返回 */
@Data
public class StudentCourseDetailVO {
    private Long teachingClassId;
    private Long offeringId;
    private String courseName;
    private String courseType;
    private String className;
    private String teacherName;
    private String termName;
    private String statusText;

    /** 页签开关：测验 / 作业 / 项目由成绩构成决定 */
    private Boolean showGuide;
    private Boolean showObjectives;
    private Boolean showCatalog;
    private Boolean showQuiz;
    private Boolean showHomework;
    private Boolean showProject;

    private String guideContent;
    private String guideFormat;
    private LocalDateTime guidePublishTime;

    private List<Objective> objectives;
    private List<StudentLessonVO> lessons;
    private List<PaperBrief> papers;
    private List<ProjectBrief> projects;

    @Data
    public static class Objective {
        private String objCode;
        private String objContent;
        private String objType;
    }

    @Data
    public static class PaperBrief {
        private Long id;
        private String paperName;
        private String paperType;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer durationMinutes;
        private BigDecimal fullScore;
        private String myStatus;
        private BigDecimal myScore;
        private Boolean canAnswer;
        private String reason;
        private Integer questionCount;
    }

    @Data
    public static class ProjectBrief {
        private Long id;
        private String projectName;
        private String description;
        private Integer isTeam;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal fullScore;
        private String myStatus;
        private BigDecimal myScore;
        private String reason;
    }
}
