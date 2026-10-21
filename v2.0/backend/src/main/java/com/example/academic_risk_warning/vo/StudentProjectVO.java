package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 项目任务（学生端） */
@Data
public class StudentProjectVO {
    private Long id;
    private Long teachingClassId;
    private String courseName;
    private String teacherName;
    private String projectName;
    private String description;
    private Integer isTeam;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal fullScore;
    private String allowFormats;
    private String status;
    private Boolean canSubmit;
    private String reason;
    private Boolean canCreateTeam;
    private ProjectTeamVO myTeam;
    private ProjectSubmissionVO mySubmission;
    private Boolean isLeader;
    private Boolean contributionSubmitted;
    private List<ProjectMemberVO> teamMembers;
}
