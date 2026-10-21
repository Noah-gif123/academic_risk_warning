package com.example.academic_risk_warning.vo;

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
