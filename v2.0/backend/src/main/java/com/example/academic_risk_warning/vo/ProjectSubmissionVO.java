package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 项目提交与批阅 */
@Data
public class ProjectSubmissionVO {
    private Long id;
    private Long projectId;
    private Long teamId;
    private String teamName;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private LocalDateTime submitTime;
    private List<Map<String, String>> files;
    private String repoUrl;
    private String remark;
    private String submitStatus;
    private BigDecimal projectScore;
    private String comment;
    private String markedByName;
    private LocalDateTime markedAt;
    /** 团队项目：各成员的占比与个人得分 */
    private List<ProjectMemberVO> members;
}
