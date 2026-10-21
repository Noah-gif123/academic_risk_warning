package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 项目任务（教师端） */
@Data
public class ProjectVO {
    private Long id;
    private Long teachingClassId;
    private String projectName;
    private String description;
    private Long lessonId;
    private Integer weekNo;
    private Integer sessionNo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer isTeam;
    private BigDecimal fullScore;
    private String allowFormats;
    private Long schemeItemId;
    private String schemeItemName;
    private String status;
    private Integer teamCount;
    private Integer submitCount;
    private Integer markedCount;
    private Integer totalStudents;
    private Integer missingContributionCount;
    private List<ProjectTeamVO> teams;
    private List<ProjectSubmissionVO> submissions;
}
