package com.example.academic_risk_warning.vo;

import com.example.academic_risk_warning.dto.QuestionDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 试卷视图（教师端） */
@Data
public class PaperVO {
    private Long id;
    private Long teachingClassId;
    private String paperType;
    private String paperName;
    private Long lessonId;
    private Integer weekNo;
    private Integer sessionNo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private BigDecimal fullScore;
    private BigDecimal questionScore;
    private Long schemeItemId;
    private String schemeItemName;
    private Integer allowViewPaper;
    private Integer showScore;
    private String showAnswer;
    private String status;
    private String remark;
    private Integer questionCount;
    private Integer subjectiveCount;
    private Integer submitCount;
    private Integer markedCount;
    private Integer absentCount;
    private Integer totalStudents;
    private List<QuestionDTO> questions;
}
