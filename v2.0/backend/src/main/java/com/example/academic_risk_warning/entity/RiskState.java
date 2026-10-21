package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 当前风险状态表（替代每日快照；一人一课一行）
 * <p>对应表：risk_state</p>
 */
@Data
@TableName("risk_state")
public class RiskState {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 选课ID（一人一课一行） */
    private Long enrollmentId;

    /** 学生ID */
    private Long studentId;

    /** 开课ID */
    private Long offeringId;

    /** 教学班ID */
    private Long teachingClassId;

    /** 学期ID */
    private Long termId;

    /** A通道：当前累计缺课次数 */
    private Integer absentCount;

    /** A通道：当前累计迟到次数 */
    private Integer lateCount;

    /** A通道：当前累计请假次数 */
    private Integer leaveCount;

    /** A通道：允许缺课上限 N_max = floor(分母课次/3) */
    private Integer allowedAbsent;

    /** A通道：剩余可缺次数 = N_max − 已缺；0=再缺一次即挂；≤−1=必然挂科 */
    private Integer remainingAbsent;

    /** A通道：是否已触发 1/3 硬门槛 */
    private Integer hardFail;

    /** A通道：是否高危（剩余可缺 ≤ 1） */
    private Integer nearFail;

    /** A通道：期末需考分数 x = (pass_score + P − W_其余项)/r_f */
    private BigDecimal finalExamNeeded;

    /** A通道：x > 期末满分 → 实际不可达 */
    private Integer finalUnreachable;

    /** A通道等级：SAFE正常/WATCH警戒/DANGER再缺一次即挂/FAIL必然挂科 */
    private String channelALevel;

    /** B通道：班内相对风险分（0–100） */
    private BigDecimal channelBScore;

    /** B通道：班内风险排名（1 最危险） */
    private Integer channelBRank;

    /** B通道：同班人数 */
    private Integer channelBClassSize;

    /** B通道等级：SAME_AS_A 语义不混算，仅作分档展示 */
    private String channelBLevel;

    /** C通道：折算后的跨班可比风险分（影子分数，不落成绩表） */
    private BigDecimal channelCScore;

    /** C通道：本次使用的教师尺度 Δ̂（收缩后） */
    private BigDecimal deltaUsed;

    /** C通道：本次使用的课程因子 c */
    private BigDecimal courseFactorUsed;

    /** C通道等级：RED/ORANGE/YELLOW/GREEN（按 engine 阈值分档） */
    private String channelCLevel;

    /** 当前对外风险分（有 C 用 C，无 C 用 B，A 单列展示） */
    private BigDecimal riskScore;

    /** 当前等级：RED/ORANGE/YELLOW/GREEN */
    private String riskLevel;

    /** 当前生效通道：A/B/C（教务看C、导员看A+B） */
    private String activeChannel;

    /** 置信度 W = 锚点强度 × 数据完备度 × 样本充足度（0–1） */
    private BigDecimal confidence;

    /** 置信度档：HIGH/MEDIUM/LOW/VERY_LOW */
    private String confidenceLevel;

    /** 输出形态：FULL_SCORE完整分档/SCORE_WITH_NOTE带标注/BEHAVIOR_ONLY仅行为预警/INSUFFICIENT数据不足 */
    private String outputMode;

    /** 数据质量明细：{"coverage":0.62,"anchorStrength":0.8,"sampleSize":120,"attendanceSourceMix":"NORMAL","missing":["mid_score"]} */
    private String dataQualityJson;

    /** 本次刷新来源：EVALUATION评估/DATA_CHANGE数据变更/MANUAL人工/WITHDRAW退课作废 */
    private String triggerSource;

    /** 最近一次刷新时间（干预"无效判定"以此为窗口起点之一） */
    private LocalDateTime lastEvalTime;

    /** 计算所用引擎版本 */
    private String engineVersion;

    /** 是否作废（退课/重算）：1作废，不进任何统计与列表 */
    private Integer isVoid;

    private String voidReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
