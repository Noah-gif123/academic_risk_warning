package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 最终成绩单（判定结果留档；折算分不落库）
 * <p>对应表：score_result</p>
 */
@Data
@TableName("score_result")
public class ScoreResult {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 选课ID */
    private Long enrollmentId;

    /** 学生ID */
    private Long studentId;

    /** 开课ID */
    private Long offeringId;

    /** 学期ID */
    private Long termId;

    /** 加权分 W = Σ(得分/满分×100×权重) */
    private BigDecimal weightedScore;

    /** 扣分 P = 2×缺课 + 1×迟到 + 1×max(0,请假−3)（无上限，不截断） */
    private BigDecimal deductionPoints;

    /** 总评 T = W − P（可为负） */
    private BigDecimal totalScore;

    /** 判定时适用的及格线（快照） */
    private Integer passScore;

    /** 判定时的缺课次数（快照，1/3 门槛的分子） */
    private Integer absentCount;

    /** 判定时的迟到次数（快照） */
    private Integer lateCount;

    /** 判定时的请假次数（快照） */
    private Integer leaveCount;

    /** 允许缺课上限 N_max = floor(分母课次/3)（快照） */
    private Integer allowedAbsent;

    /** 判定时使用的分母课次（教务确认后的实际课次快照） */
    private Integer effectiveSessions;

    /** 是否触发 1/3 硬门槛：1是（不看分数，直接挂科） */
    private Integer hardFail;

    /** 是否挂科：T < 及格线 或 触发硬门槛 */
    private Integer isFail;

    /** 挂科原因：SCORE分数不足/HARD_LINE缺课超1/3/NO_SCORE无成绩 */
    private String failReason;

    /** 成绩状态：PENDING待定/FINAL已定/DEFERRED缓考/DEFERRED_GIVEUP缓考后弃考/ABSENT_EXAM旷考/NO_SCORE成绩未齐 */
    private String scoreStatus;

    /** 重修次数（0首次） */
    private Integer retakeTimes;

    /** 计算所用引擎版本（规则/权重/公式变化即升版） */
    private String engineVersion;

    /** 本次计算结果时间 */
    private LocalDateTime calculatedAt;

    /** 是否作废（退课、重算覆盖等） */
    private Integer isVoid;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
