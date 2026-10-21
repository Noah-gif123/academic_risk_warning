package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 项目组队表
 * <p>对应表：project_team</p>
 */
@Data
@TableName("project_team")
public class ProjectTeam {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 项目任务ID */
    private Long projectId;

    /** 组名 */
    private String teamName;

    /** 组长学生ID（贡献表提交人） */
    private Long leaderStudentId;

    /** 成员列表快照 */
    private String memberJson;

    /** 成员数（冗余，便于校验） */
    private Integer memberCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
