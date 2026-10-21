package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 课程详情·教学统计 */
@Data
public class CourseStatsVO {
    private Long teachingClassId;
    private String courseName;
    private String className;
    private String teacherName;
    private Integer studentCount;

    /** 今日登录过的学生数 / 占比 */
    private Integer activeToday;
    private BigDecimal activeRate;

    /** 累计出勤率（出勤 + 迟到）/(已点名课次) */
    private BigDecimal attendanceRate;
    private Integer calledSessions;
    private Integer effectiveSessions;

    /** 已发布的任务数量 */
    private Integer materialCount;
    private Integer quizCount;
    private Integer homeworkCount;
    private Integer projectCount;

    /** 四类任务的未开始 / 进行中 / 已截止 */
    private Integer finishedQuiz;
    private Integer runningQuiz;
    private Integer notStartedQuiz;
    private Integer finishedHomework;
    private Integer runningHomework;
    private Integer notStartedHomework;
    private Integer finishedProject;
    private Integer runningProject;
    private Integer notStartedProject;

    /** 课程平均成绩（按已定稿成绩单） */
    private BigDecimal avgScore;
    private Integer scoredCount;
    private BigDecimal passRate;
}
