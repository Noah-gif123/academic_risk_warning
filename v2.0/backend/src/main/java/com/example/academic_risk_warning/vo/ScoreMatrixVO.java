package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.util.List;

/** 成绩矩阵（列 = 评分项，行 = 学生） */
@Data
public class ScoreMatrixVO {
    private Long teachingClassId;
    private String courseName;
    private String className;
    private String teacherName;
    private List<ScoreItemColumnVO> items;
    private List<ScoreRowVO> rows;
    private ScoreSummaryVO summary;
}
