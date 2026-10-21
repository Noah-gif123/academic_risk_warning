package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 目录一行 = 一次课，含课前 / 课中 / 课后三段 */
@Data
public class LessonVO {
    private Long id;
    private Long sessionId;
    private Integer weekNo;
    private Integer sessionNo;
    private LocalDate sessionDate;
    private String title;
    private Integer sortNo;
    private String status;
    private List<LessonResourceVO> before;
    private List<LessonResourceVO> during;
    private List<LessonResourceVO> after;
}
