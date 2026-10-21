package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 教师首页：我的教学课程 + 待批改（作业 / 测试 / 项目三类） */
@Data
public class TeacherHomeVO {
    private List<TeacherCourseCardVO> courses;
    private List<PendingItem> pendingHomeworks;
    private List<PendingItem> pendingQuizzes;
    private List<PendingItem> pendingProjects;
    private Integer homeworkPending;
    private Integer quizPending;
    private Integer projectPending;

    @Data
    public static class PendingItem {
        private String kind;
        private Long id;
        private Long teachingClassId;
        private String courseName;
        private String className;
        private String title;
        private Integer pendingCount;
        private LocalDateTime endTime;
        private BigDecimal fullScore;
        private Long submissionId;
    }
}
