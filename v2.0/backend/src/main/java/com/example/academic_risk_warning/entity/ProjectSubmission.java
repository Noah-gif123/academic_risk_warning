package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 项目提交与批阅表
 * <p>对应表：project_submission</p>
 */
@Data
@TableName("project_submission")
public class ProjectSubmission {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 项目任务ID */
    private Long projectId;

    /** 组ID（个人项目为空） */
    private Long teamId;

    /** 提交人（团队项目=组长） */
    private Long studentId;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 提交文件列表 */
    private String filesJson;

    /** 代码仓库链接（可选） */
    private String repoUrl;

    /** 提交说明 */
    private String remark;

    /** 状态：NOT_SUBMITTED未提交/SUBMITTED已提交/MARKED已批阅 */
    private String submitStatus;

    /** 教师给的项目总分（团队项目为整组分） */
    private BigDecimal projectScore;

    /** 教师评语 */
    private String comment;

    /** 批阅教师ID */
    private Long markedBy;

    /** 批阅时间 */
    private LocalDateTime markedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
