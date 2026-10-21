/* gen_counselor_module.js — 第一阶段 1-14 导员端：看板 / 名单与单科详情 / 干预记录 / 问卷 / 通知 */
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

// ---------------------------------------------------------------- dto
w('dto/InterventionSaveRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 导员录入干预记录 */
@Data
public class InterventionSaveRequest {
    @NotNull(message = "学生不能为空")
    private Long studentId;
    /** TALK 谈话 / MEETING 班会 / PARENT 联系家长 / OTHER 其他 */
    private String actionType;
    private String content;
    private String result;
    /** 处理是否有效：EFFECTIVE / INEFFECTIVE / PENDING */
    private String effective;
    private String remark;
}
`);

w('dto/ReferRequest.java', `
package ${P}.dto;

import lombok.Data;

/** 两次处理无效后上报教务 / 终止干预 */
@Data
public class ReferRequest {
    private String note;
    private String reason;
}
`);

w('dto/QuestionnaireSaveRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 导员按行政班发布「教师风格判定」问卷 */
@Data
public class QuestionnaireSaveRequest {
    @NotNull(message = "行政班不能为空")
    private Long adminClassId;
    private String title;
    private Long offeringId;
    private Long courseId;
}
`);

// ---------------------------------------------------------------- vo
w('vo/CounselorClassSummaryVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 导员名下行政班的概况 */
@Data
public class CounselorClassSummaryVO {
    private Long adminClassId;
    private String classCode;
    private String className;
    private Integer gradeYear;
    private String majorName;
    private Integer studentCount;
    private Integer scoredCount;
    private Integer failCount;
    private Integer hardFailCount;
    private BigDecimal failRate;
    private Integer absentOverCount;
    private Integer riskStudentCount;
    /** 班集体提醒等级：NONE / SUGGEST（≥30% 且 ≥5 人）/ FOCUS（≥50% 且 ≥10 人） */
    private String classAlertLevel;
    private String classAlertText;
    private Integer weekNo;
}
`);

w('vo/CounselorStudentVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 导员看到的学生行（跨课程聚合） */
@Data
public class CounselorStudentVO {
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Long adminClassId;
    private String adminClassName;
    /** 有风险的课程数（挂科或触发缺课门槛） */
    private Integer riskCourseCount;
    private Integer failCourseCount;
    private Integer hardFailCourseCount;
    private Integer courseCount;
    private BigDecimal minTotalScore;
    private BigDecimal avgTotalScore;
    private Integer absentTotal;
    /** 危险度（用于排序，越大越危险） */
    private BigDecimal riskScore;
    private String riskHint;
    private Long latestInterventionId;
    private String latestInterventionText;
}
`);

w('vo/CounselorStudentDetailVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 学生单科详情 + 处理记录 */
@Data
public class CounselorStudentDetailVO {
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String majorName;
    private String adminClassName;
    private Integer enrollYear;
    private String phone;
    private List<CourseRow> courses;
    private List<InterventionRow> interventions;

    @Data
    public static class CourseRow {
        private Long teachingClassId;
        private String courseName;
        private String teacherName;
        private Integer effectiveSessions;
        private Integer allowedAbsent;
        private Integer absentCount;
        private Integer lateCount;
        private Integer leaveCount;
        private Integer deductPoints;
        private BigDecimal weightedScore;
        private BigDecimal totalScore;
        private Integer passScore;
        private Boolean hardFail;
        private Boolean fail;
        private String failReasonText;
        private String scoreStatus;
        private String riskLevel;
        private String riskHint;
    }

    @Data
    public static class InterventionRow {
        private Long id;
        private String actionType;
        private String actionTypeText;
        private String content;
        private String result;
        private String effective;
        private String effectiveText;
        private String operatorRole;
        private String referStatus;
        private String referNote;
        private String adminFeedback;
        private LocalDateTime createTime;
    }
}
`);

w('vo/CounselorOverviewVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 导员首页看板 */
@Data
public class CounselorOverviewVO {
    private String termName;
    private Integer weekNo;
    private Integer classCount;
    private Integer studentCount;
    private Integer riskStudentCount;
    private Integer failStudentCount;
    private Integer hardFailStudentCount;
    private BigDecimal avgFailRate;
    private List<CounselorClassSummaryVO> classes;
    private List<CounselorStudentVO> focusStudents;
    private List<String> reminders;
}
`);

w('vo/QuestionnaireVO.java', `
package ${P}.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 问卷（当前只有「教师风格判定」一种，匿名） */
@Data
public class QuestionnaireVO {
    private Long id;
    private String title;
    private String questionnaireType;
    private String questionnaireTypeText;
    private Long adminClassId;
    private String adminClassName;
    private String courseName;
    private Boolean anonymous;
    private String status;
    private String statusText;
    private Integer answerCount;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private String collectorType;
    private LocalDateTime createTime;
}
`);

w('vo/CounselorNoticeVO.java', `
package ${P}.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 通知与回执 */
@Data
public class CounselorNoticeVO {
    private Long id;
    private String bizType;
    private Long bizId;
    private String title;
    private String content;
    private String audienceDesc;
    private String priority;
    private Integer aggregateCount;
    private LocalDateTime createTime;
    private Integer isRead;
    private LocalDateTime readTime;
}
`);

w('vo/CounselorProfileVO.java', `
package ${P}.vo;

import lombok.Data;

/** 导员个人中心 */
@Data
public class CounselorProfileVO {
    private Long counselorId;
    private String counselorNo;
    private String counselorName;
    private String collegeName;
    private String phone;
    private String email;
    private String accountNo;
    private Integer classCount;
}
`);

console.log('导员端模块：dto / vo 生成完成');
