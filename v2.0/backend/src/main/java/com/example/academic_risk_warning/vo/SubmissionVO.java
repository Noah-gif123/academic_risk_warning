package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 提交记录（教师批阅 / 学生看结果） */
@Data
public class SubmissionVO {
    private Long id;
    private Long paperId;
    private String paperName;
    private String paperType;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String submitStatus;
    private String markedStatus;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private Integer durationSeconds;
    private BigDecimal objectiveScore;
    private BigDecimal subjectiveScore;
    private BigDecimal totalScore;
    private BigDecimal fullScore;
    private Integer pendingCount;
    private Integer isAbsent;
    private String remark;
    private String markedByName;
    private List<SubmissionAnswerVO> answers;
}
