/* gen_attendance_module.js — 第一阶段 1-9 考勤与归档（点名 / 行级留痕 / 汇总与 1/3 门槛 / 归档批次 / 覆盖率督办 / 导入） */
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
w('dto/AttendanceItemDTO.java', `
package ${P}.dto;

import lombok.Data;

/** 点名单中的一行：学生 + 考勤状态 */
@Data
public class AttendanceItemDTO {
    private Long studentId;
    /** PRESENT / LATE / ABSENT / LEAVE */
    private String status;
    /** PERSONAL / SICK / OFFICIAL（仅请假时填） */
    private String leaveType;
}
`);

w('dto/AttendanceCallRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 一次点名提交（一节课一个批次，默认全班出勤，只提交异常） */
@Data
public class AttendanceCallRequest {
    @NotNull(message = "教学班不能为空")
    private Long teachingClassId;
    @NotNull(message = "课次不能为空")
    private Long sessionId;
    private String note;
    private List<AttendanceItemDTO> items;
}
`);

w('dto/AttendanceRecordUpdateRequest.java', `
package ${P}.dto;

import lombok.Data;

/** 单行修改（平时可直接改；归档后只有教务能改，且必须写原因） */
@Data
public class AttendanceRecordUpdateRequest {
    private String status;
    private String leaveType;
    private String note;
}
`);

w('dto/ArchiveSubmitRequest.java', `
package ${P}.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 教师提交归档（期中 / 期末） */
@Data
public class ArchiveSubmitRequest {
    @NotNull(message = "教学班不能为空")
    private Long teachingClassId;
    /** MID / FINAL */
    @NotBlank(message = "归档阶段不能为空")
    private String phase;
    private String note;
}
`);

w('dto/ArchiveReviewRequest.java', `
package ${P}.dto;

import lombok.Data;

/** 教务审核归档：通过 / 退回 */
@Data
public class ArchiveReviewRequest {
    private String note;
    private String reason;
}
`);

// ================================================================ vo
w('vo/AttendanceSessionVO.java', `
package ${P}.vo;

import lombok.Data;

import java.time.LocalDate;

/** 点名用的课次行 */
@Data
public class AttendanceSessionVO {
    private Long sessionId;
    private Integer weekNo;
    private Integer sessionNo;
    private LocalDate sessionDate;
    private String sessionType;
    private String status;
    private String rollCallStatus;
    private Integer rollCallCount;
    private Integer totalStudents;
    private Integer presentCount;
    private Integer lateCount;
    private Integer absentCount;
    private Integer leaveCount;
    private Boolean archived;
    private String phase;
}
`);

w('vo/AttendanceRosterRowVO.java', `
package ${P}.vo;

import lombok.Data;

/** 点名单一行：学生 + 当前状态（未点名时默认出勤） */
@Data
public class AttendanceRosterRowVO {
    private Long enrollmentId;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String status;
    private String leaveType;
    private Long recordId;
    private Boolean archived;
    private Integer absentTotal;
    private String thresholdHint;
}
`);

w('vo/AttendanceStudentStatVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 一个学生在本教学班的考勤汇总（扣分不落库，按口径实时算） */
@Data
public class AttendanceStudentStatVO {
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Integer presentCount;
    private Integer lateCount;
    private Integer absentCount;
    private Integer leaveCount;
    private Integer calledSessions;
    /** P = 2×缺课 + 1×迟到 + 1×max(0, 请假−3)，无上限 */
    private Integer deductPoints;
    /** N_max = floor(有效课次 / 3) */
    private Integer maxAbsent;
    private Integer remainingAbsent;
    private String thresholdHint;
    private BigDecimal attendanceRate;
}
`);

w('vo/CoverageVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 点名覆盖率（教务督办用）：低于 1/3 判低置信度并催办 */
@Data
public class CoverageVO {
    private Long teachingClassId;
    private String classCode;
    private String className;
    private String courseName;
    private String teacherName;
    private Integer effectiveSessions;
    private Integer passedSessions;
    private Integer calledSessions;
    private BigDecimal coverageRate;
    private Boolean lowCoverage;
    private Integer archivePending;
}
`);

w('vo/ArchiveVO.java', `
package ${P}.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 归档批次视图 */
@Data
public class ArchiveVO {
    private Long id;
    private Long offeringId;
    private Long teachingClassId;
    private String classCode;
    private String className;
    private String courseName;
    private String termName;
    private String teacherName;
    private String phase;
    private String status;
    private LocalDateTime submitTime;
    private String submitNote;
    private Integer sessionFrom;
    private Integer sessionTo;
    private Integer sessionCount;
    private Integer recordCount;
    private BigDecimal coverageRate;
    private LocalDateTime reviewTime;
    private String reviewNote;
    private LocalDateTime returnTime;
    private String returnReason;
    private Integer remindCount;
    private LocalDateTime deadline;
}
`);

w('vo/AttendanceImportVO.java', `
package ${P}.vo;

import lombok.Data;

import java.util.List;

/** 批量导入点名单的结果 */
@Data
public class AttendanceImportVO {
    private String fileName;
    private Integer totalRows;
    private Integer successRows;
    private Integer skippedRows;
    private List<String> errors;
}
`);

console.log('考勤模块：dto / vo 生成完成');
