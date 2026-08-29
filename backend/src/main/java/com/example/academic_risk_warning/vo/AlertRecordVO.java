package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预警记录VO
 */
@Data
public class AlertRecordVO {

    private Long id;

    private Long studentId;

    private String studentName;

    private String studentNo;

    private Long courseId;

    private String courseName;

    /** 预警等级 */
    private String alertLevel;

    /** 预警等级显示名称 */
    private String alertLevelName;

    /** 预警类型 */
    private String alertType;

    /** 预警类型显示名称 */
    private String alertTypeName;

    /** 预警原因 */
    private String alertReason;

    /** 综合风险分数 */
    private BigDecimal riskScore;

    /** 预测成绩 */
    private BigDecimal predictedScore;

    /** 各维度风险分 */
    private BigDecimal academicRiskScore;
    private BigDecimal homeworkRiskScore;
    private BigDecimal attendanceRiskScore;
    private BigDecimal knowledgeRiskScore;
    private BigDecimal historyRiskScore;

    /** 预警状态 */
    private String status;

    /** 状态显示名称 */
    private String statusName;

    /** 教师处理备注 */
    private String teacherNote;

    /** 教师处理时间 */
    private LocalDateTime handleTime;

    /** 学生确认时间 */
    private LocalDateTime ackTime;

    /** 预警生成时间 */
    private LocalDateTime createTime;

    /** 预警失效时间 */
    private LocalDateTime expireTime;

    /** 是否已发送通知 */
    private Boolean notified;

    /** 关联的干预类型名称列表(详情接口返回) */
    private List<String> interventionTypes;

    /** 操作日志列表(详情接口返回) */
    private List<OpLog> operationLogs;

    @Data
    public static class OpLog {
        private String operatorType;
        private String operationName;
        private String remark;
        private LocalDateTime createTime;
    }
}
