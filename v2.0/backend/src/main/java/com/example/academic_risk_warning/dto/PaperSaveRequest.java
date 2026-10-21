package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 新建 / 保存试卷（草稿） */
@Data
public class PaperSaveRequest {
    private Long id;
    @NotNull(message = "教学班不能为空")
    private Long teachingClassId;
    /** QUIZ / HOMEWORK / EXAM */
    @NotBlank(message = "类型不能为空")
    private String paperType;
    @NotBlank(message = "名称不能为空")
    private String paperName;
    private Long lessonId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private BigDecimal fullScore;
    private Long schemeItemId;
    private Integer allowViewPaper;
    private Integer showScore;
    /** NEVER / AFTER_MARK */
    private String showAnswer;
    private String remark;
    private List<QuestionDTO> questions;
}
