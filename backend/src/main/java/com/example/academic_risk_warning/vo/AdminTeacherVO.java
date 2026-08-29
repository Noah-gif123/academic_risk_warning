package com.example.academic_risk_warning.vo;

import lombok.Data;

/**
 * 管理员看板 — 教师总览（含风险统计）
 */
@Data
public class AdminTeacherVO {

    private Long teacherId;
    private String teacherNo;
    private String teacherName;
    private String phone;
    private String className;           // 负责班级
    private Integer studentCount;       // 名下学生总数
    private Integer totalAlertCount;    // 活跃预警总数
    private Integer redAlertCount;      // 红色预警数
    private Integer orangeAlertCount;   // 橙色预警数
    private Integer yellowAlertCount;   // 黄色预警数
    private Integer handledCount;       // 已处理预警数
    private double handleRate;          // 预警处理率 0.0~1.0
    private String managementStatus;    // 管理状态: NORMAL/ATTENTION/WARNING/RECTIFY/RESOLVED
}
