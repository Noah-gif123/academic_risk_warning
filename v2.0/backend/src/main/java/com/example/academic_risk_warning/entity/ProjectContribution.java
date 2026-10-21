package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 团队贡献表（组长提交，不可修改）
 * <p>对应表：project_contribution</p>
 */
@Data
@TableName("project_contribution")
public class ProjectContribution {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 项目任务ID */
    private Long projectId;

    /** 组ID */
    private Long teamId;

    /** 组长学生ID（提交人） */
    private Long leaderStudentId;

    /** 成员贡献占比列表 */
    private String contributionJson;

    /** 占比合计（必须=1.0000） */
    private BigDecimal totalRatio;

    /** 提交时间（提交后不可改） */
    private LocalDateTime submitTime;

    /** 是否锁定：1提交即锁定（不允许任何角色修改） */
    private Integer isLocked;

    /** 组长备注 */
    private String remark;

    private LocalDateTime createTime;
}
