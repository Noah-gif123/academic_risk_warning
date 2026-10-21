package com.example.academic_risk_warning.dto;

import lombok.Data;

/**
 * 合成数据生成参数（默认就是课题要求的口径：720 学生 / 6 门课 / 每生 6 门 / 32 课次）。
 * 注意：生成到独立学期（默认 SYN-2027-1），不影响演示学期。
 */
@Data
public class SyntheticGenerateRequest {
    private String termCode;
    private String termName;
    private Integer studentCount;
    private Integer courseCount;
    private Integer sessionsPerCourse;
    private Integer classesPerCourse;
    private Integer attendanceSessions;
    private Boolean generateScores;
    /** 重新生成前清掉本生成器造的数据（只动该学期） */
    private Boolean reset;
}
