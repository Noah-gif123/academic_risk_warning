package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 教师端风险名单的一行（教师端「预警监控」页面直接渲染这个结构） */
@Data
public class RiskRowVO {
    private Long riskStateId;
    private Long enrollmentId;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Long offeringId;
    private String courseName;
    private Long teachingClassId;
    private String classCode;
    private String className;
    private Long termId;

    /** A 通道：缺课 / 允许 / 剩余可缺 */
    private Integer absentCount;
    private Integer lateCount;
    private Integer leaveCount;
    private Integer allowedAbsent;
    private Integer remainingAbsent;
    /** A 通道：是否触发 1/3 硬门槛、是否临近门槛 */
    private Integer hardFail;
    private Integer nearFail;
    /** M16：期末/大作业需考分与是否可达（过程分口径算出来的） */
    private BigDecimal finalExamNeeded;
    private Integer finalUnreachable;
    private String channelALevel;

    /** M16：过程分（非终结性分支加权平均，0–100）与它的覆盖率 */
    private BigDecimal processScore;
    private BigDecimal processCoverage;

    /** B 通道：绝对口径基础分与班内排名（排名只用于排序，不是预警结论） */
    private BigDecimal channelBScore;
    private Integer channelBRank;
    private Integer channelBClassSize;

    /** 对外风险分与等级 */
    private BigDecimal riskScore;
    private String riskLevel;
    private String activeChannel;

    /** 置信度与输出形态 */
    private BigDecimal confidence;
    private String confidenceLevel;
    private String outputMode;

    private String engineVersion;
    private LocalDateTime lastEvalTime;
}
