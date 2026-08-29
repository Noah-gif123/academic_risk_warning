package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 学生全套学情信息
 */
@Data
public class StudentFullVO {

    private StudentBasicVO basicInfo;

    private List<ScoreInfoVO> scores = new ArrayList<>();

    private List<HomeworkInfoVO> homeworkList = new ArrayList<>();

    private List<ClassPerformanceVO> classPerformances = new ArrayList<>();

    private List<HistoryRiskVO> historyRisks = new ArrayList<>();

    private List<KnowledgeMasteryVO> knowledgeMasteries = new ArrayList<>();

    /** 薄弱知识点明细（来自 student_weak_point）：kpName/errorRate/errorCount/courseId/courseName */
    private List<Map<String, Object>> weakPoints = new ArrayList<>();

    /** 学生整体综合成绩（各科目calculatedScore的加权平均） */
    private Double overallScore;
}
