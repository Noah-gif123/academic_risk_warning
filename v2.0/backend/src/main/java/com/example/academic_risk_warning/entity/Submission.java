package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 提交与作答记录表
 * <p>对应表：submission</p>
 */
@Data
@TableName("submission")
public class Submission {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 试卷ID */
    private Long paperId;

    /** 教学班ID（冗余） */
    private Long teachingClassId;

    /** 学期ID */
    private Long termId;

    /** 学生ID */
    private Long studentId;

    /** 选课ID（过程数据挂在选课上） */
    private Long enrollmentId;

    /** 状态：SUBMITTED已交/MARKED已批阅/ABSENT缺考 */
    private String submitStatus;

    /** 开考时间 */
    private LocalDateTime startTime;

    /** 交卷时间 */
    private LocalDateTime submitTime;

    /** 答题用时（秒） */
    private Integer durationSeconds;

    /** 客观题得分（系统判） */
    private BigDecimal objectiveScore;

    /** 主观题得分（教师判） */
    private BigDecimal subjectiveScore;

    /** 总分（批阅完成后写入） */
    private BigDecimal totalScore;

    /** 批阅进度：NOT_MARKED/SYSTEM_MARKED客观已判/ALL_MARKED全部批完 */
    private String markedStatus;

    /** 最后批阅人（教师ID；系统判分为空） */
    private Long markedBy;

    /** 最后批阅时间 */
    private LocalDateTime markedAt;

    /** 是否缺考：1是（记 0 分） */
    private Integer isAbsent;

    /** 整卷批注 */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
