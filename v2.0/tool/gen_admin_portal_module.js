/* gen_admin_portal_module.js — 第一阶段 1-13 教务端：工作台 / 导入批次 / 入课知会 / 审计 / 覆盖率督办 */
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

w('vo/AdminDashboardVO.java', `
package ${P}.vo;

import lombok.Data;

/** 教务工作台概览 */
@Data
public class AdminDashboardVO {
    private String currentTermName;
    private Long currentTermId;
    private Integer offeringCount;
    private Integer teachingClassCount;
    private Integer studentCount;
    private Integer teacherCount;
    private Integer courseCount;
    /** 待教务审核的归档批次 */
    private Integer archivePending;
    /** 点名覆盖率低于 1/3 的教学班数 */
    private Integer lowCoverageCount;
    /** 待查看的入课知会 */
    private Integer joinNoticeCount;
    /** 导入批次数 */
    private Integer importBatchCount;
    /** 今日审计条数 */
    private Integer auditTodayCount;
}
`);

w('vo/ImportBatchVO.java', `
package ${P}.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 导入批次（可整批回滚） */
@Data
public class ImportBatchVO {
    private Long id;
    private String batchNo;
    private String importType;
    private String fileName;
    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;
    private String status;
    private String statusText;
    private String operatorName;
    private String operatorRole;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime rollbackTime;
    private String rollbackReason;
    private String remark;
    /** 失败明细（最多前 10 条） */
    private java.util.List<String> errors;
}
`);

w('vo/AuditLogVO.java', `
package ${P}.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 审计日志（改分、改考勤、归档、退回、上报等关键操作） */
@Data
public class AuditLogVO {
    private Long id;
    private String bizType;
    private Long bizId;
    private String action;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String operatorRoleText;
    private String detail;
    private LocalDateTime createTime;
}
`);

w('vo/AuditLogPageVO.java', `
package ${P}.vo;

import lombok.Data;

import java.util.List;

/** 审计日志分页 */
@Data
public class AuditLogPageVO {
    private List<AuditLogVO> items;
    private Integer total;
    private Integer page;
    private Integer size;
    private Integer pages;
    private List<String> bizTypes;
    private List<String> actions;
}
`);

console.log('教务端模块：vo 生成完成');
