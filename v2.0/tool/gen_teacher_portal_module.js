/* gen_teacher_portal_module.js — 第一阶段 1-12 教师端：首页汇总 / 课程详情（三页签）/ 教学详情（六页签）所需数据 */
'use strict';
const fs = require('fs');
const path = require('path');

const BE = 'E:/JavaDEVIDEA/workspace/JavaProject/academic_risk_warning/v2.0/backend';
const J = path.join(BE, 'src/main/java/com/example/academic_risk_warning');
const P = 'com.example.academic_risk_warning';

function w(rel, content) {
  const p = path.join(J, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, content.replace(/^\n/, ''), 'utf8');
}

w('vo/TeacherCourseCardVO.java', `
package ${P}.vo;

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
`);

w('vo/TeacherHomeVO.java', `
package ${P}.vo;

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
`);

w('vo/CourseStatsVO.java', `
package ${P}.vo;

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
`);

console.log('教师端模块：vo 生成完成');
