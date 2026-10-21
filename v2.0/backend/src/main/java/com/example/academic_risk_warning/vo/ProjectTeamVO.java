package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 项目组：成员 + 贡献表状态 */
@Data
public class ProjectTeamVO {
    private Long id;
    private String teamName;
    private Long leaderStudentId;
    private String leaderStudentNo;
    private String leaderStudentName;
    private Integer memberCount;
    private List<ProjectMemberVO> members;
    private Boolean contributionLocked;
    private BigDecimal totalRatio;
    private LocalDateTime contributionTime;
    private String contributionRemark;
}
