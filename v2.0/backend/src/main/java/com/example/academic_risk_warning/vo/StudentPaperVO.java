package com.example.academic_risk_warning.vo;

import com.example.academic_risk_warning.dto.QuestionDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 学生端试卷（作答用，不含答案） */
@Data
public class StudentPaperVO {
    private Long id;
    private String paperName;
    private String paperType;
    private String courseName;
    private String teacherName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private BigDecimal fullScore;
    private Integer remainingSeconds;
    private String myStatus;
    private BigDecimal myScore;
    private Boolean canAnswer;
    private String reason;
    private List<QuestionDTO> questions;
}
