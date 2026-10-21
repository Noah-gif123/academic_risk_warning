/* gen_student_portal_module.js — 第一阶段 1-11 学生端：课程卡片 / 课程详情六页签 / 加入课程（邀请码）/ 个人中心 */
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

// ================================================================ dto
w('dto/JoinRequestDTO.java', `
package ${P}.dto;

import lombok.Data;

/** 学生用邀请码申请加入教学班 */
@Data
public class JoinRequestDTO {
    private String inviteCode;
}
`);

w('dto/JoinReviewDTO.java', `
package ${P}.dto;

import lombok.Data;

/** 教师审批入班申请 */
@Data
public class JoinReviewDTO {
    private String note;
}
`);

w('dto/InviteCodeDTO.java', `
package ${P}.dto;

import lombok.Data;

/** 教师设置邀请码与是否开启入班审核 */
@Data
public class InviteCodeDTO {
    private String inviteCode;
    private Integer joinReviewEnabled;
}
`);

// ================================================================ vo
w('vo/CourseCardVO.java', `
package ${P}.vo;

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
`);

w('vo/CourseCardPageVO.java', `
package ${P}.vo;

import lombok.Data;

import java.util.List;

/** 分页结果 */
@Data
public class CourseCardPageVO {
    private List<CourseCardVO> items;
    private Integer total;
    private Integer page;
    private Integer size;
    private Integer pages;
}
`);

w('vo/StudentLessonVO.java', `
package ${P}.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 学生端目录：一行 = 一次课，含课前 / 课中 / 课后（只含已发布资源） */
@Data
public class StudentLessonVO {
    private Long lessonId;
    private Integer weekNo;
    private Integer sessionNo;
    private LocalDate sessionDate;
    private String title;
    private List<Res> before;
    private List<Res> during;
    private List<Res> after;

    @Data
    public static class Res {
        private Long id;
        private String resType;
        private String title;
        private Long refId;
        private String extUrl;
        private Long fileId;
        private String fileExt;
        private String paperType;
        /** 试卷相关：待交 / 已交 / 已批 / 缺考 */
        private String taskStatus;
        private String projectName;
    }
}
`);

w('vo/StudentCourseDetailVO.java', `
package ${P}.vo;

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
`);

w('vo/StudentProfileVO.java', `
package ${P}.vo;

import lombok.Data;

/** 学生个人中心 */
@Data
public class StudentProfileVO {
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Integer gender;
    private Integer enrollYear;
    private String majorName;
    private String className;
    private String phone;
    private String email;
    private String accountNo;
}
`);

w('vo/JoinRequestVO.java', `
package ${P}.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 入班申请（学生端看自己的、教师端看本班的） */
@Data
public class JoinRequestVO {
    private Long id;
    private Long teachingClassId;
    private String courseName;
    private String className;
    private String teacherName;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String inviteCode;
    private String status;
    private String statusText;
    private LocalDateTime applyTime;
    private LocalDateTime reviewTime;
    private String reviewNote;
    private Boolean notifyAdmin;
}
`);

console.log('学生端模块：dto / vo 生成完成');
