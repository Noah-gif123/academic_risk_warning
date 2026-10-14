package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警记录表
 */
@Data
@TableName("alert_record")
public class AlertRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预警学生ID */
    private Long studentId;

    /** 预警课程ID（可为空，表示综合预警） */
    private Long courseId;

    /** 预警等级：RED-红色（高风险）、ORANGE-橙色（中风险）、YELLOW-黄色（低风险）、GREEN-绿色（正常） */
    private String alertLevel;

    /** 预警类型：FAILURE-挂科风险、HOMEWORK-作业欠交、KNOWLEDGE-知识点断层、ABSENTEEISM-学习倦怠、DROP-成绩骤降、CUMULATIVE-累积风险 */
    private String alertType;

    /** 预警原因描述 */
    private String alertReason;

    /** 综合风险分数（0-100） */
    private BigDecimal riskScore;

    /** 预测成绩（期末预测值） */
    private BigDecimal predictedScore;

    /** 学业风险分 */
    private BigDecimal academicRiskScore;

    /** 作业风险分 */
    private BigDecimal homeworkRiskScore;

    /** 出勤风险分 */
    private BigDecimal attendanceRiskScore;

    /** 知识风险分 */
    private BigDecimal knowledgeRiskScore;

    /** 历史风险分 */
    private BigDecimal historyRiskScore;

    /** 预警状态：ACTIVE-生效中、ACKNOWLEDGED-已确认、HANDLED-已处理、DISMISSED-已撤销 */
    private String status;

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
}
