package com.example.academic_risk_warning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 学业预警系统可配置化参数
 */
@Component
@ConfigurationProperties(prefix = "warning")
public class WarningSystemProperties {

    /** 预警默认过期天数 */
    private int alertExpireDays = 7;

    /** 学习时长分风险阈值（分钟） */
    private List<Integer> studyDurationThresholds = List.of(60, 120, 180, 240, 300);

    /** 预测成绩 - 作业提交率调整系数 */
    private double homeworkSubmitAdjustFactor = 0.3;

    /** 预测成绩 - 出勤率调整系数 */
    private double attendanceAdjustFactor = 0.2;

    /** 干预有效判定：风险分下降阈值 */
    private double interventionImproveThreshold = -10.0;

    /** 干预有效判定：当前风险分低于该值 */
    private double interventionLowRiskThreshold = 15.0;

    /** 已处理预警跳过重新生成的时间窗口（天） */
    private int handledSkipDays = 7;

    /** CLOSED 预警同学期判定窗口（月） */
    private int closedSemesterMonths = 6;

    /** 催办：ACTIVE 预警超时天数（第一次催办） */
    private int escalateActiveFirstDays = 2;

    /** 催办：ACTIVE 预警超时天数（第二次严重提醒） */
    private int escalateActiveSecondDays = 5;

    /** 催办：ACKNOWLEDGED 后催办天数 */
    private int escalateAckedDays = 3;

    /** 催办：HANDLED 后催学生回应天数 */
    private int escalateHandledFirstDays = 3;

    /** 升级：HANDLED 后学生未回应升级天数 */
    private int escalateHandledUpgradeDays = 5;

    /** 文件上传目录 */
    private String uploadDir = "uploads/exercises/";

    /** 文件上传大小限制（MB） */
    private long maxFileSizeMB = 10;

    /** 薄弱知识点判定：错误率阈值（%），高于此值视为薄弱 */
    private double weakPointErrorRateThreshold = 40.0;

    /** 练习评分及格线（0-100），低于此分视为不合格 */
    private int passScoreThreshold = 60;

    // ==================== getters / setters ====================

    public int getAlertExpireDays() { return alertExpireDays; }
    public void setAlertExpireDays(int v) { this.alertExpireDays = v; }

    public List<Integer> getStudyDurationThresholds() { return studyDurationThresholds; }
    public void setStudyDurationThresholds(List<Integer> v) { this.studyDurationThresholds = v; }

    public double getHomeworkSubmitAdjustFactor() { return homeworkSubmitAdjustFactor; }
    public void setHomeworkSubmitAdjustFactor(double v) { this.homeworkSubmitAdjustFactor = v; }

    public double getAttendanceAdjustFactor() { return attendanceAdjustFactor; }
    public void setAttendanceAdjustFactor(double v) { this.attendanceAdjustFactor = v; }

    public double getInterventionImproveThreshold() { return interventionImproveThreshold; }
    public void setInterventionImproveThreshold(double v) { this.interventionImproveThreshold = v; }

    public double getInterventionLowRiskThreshold() { return interventionLowRiskThreshold; }
    public void setInterventionLowRiskThreshold(double v) { this.interventionLowRiskThreshold = v; }

    public int getHandledSkipDays() { return handledSkipDays; }
    public void setHandledSkipDays(int v) { this.handledSkipDays = v; }

    public int getClosedSemesterMonths() { return closedSemesterMonths; }
    public void setClosedSemesterMonths(int v) { this.closedSemesterMonths = v; }

    public int getEscalateActiveFirstDays() { return escalateActiveFirstDays; }
    public void setEscalateActiveFirstDays(int v) { this.escalateActiveFirstDays = v; }

    public int getEscalateActiveSecondDays() { return escalateActiveSecondDays; }
    public void setEscalateActiveSecondDays(int v) { this.escalateActiveSecondDays = v; }

    public int getEscalateAckedDays() { return escalateAckedDays; }
    public void setEscalateAckedDays(int v) { this.escalateAckedDays = v; }

    public int getEscalateHandledFirstDays() { return escalateHandledFirstDays; }
    public void setEscalateHandledFirstDays(int v) { this.escalateHandledFirstDays = v; }

    public int getEscalateHandledUpgradeDays() { return escalateHandledUpgradeDays; }
    public void setEscalateHandledUpgradeDays(int v) { this.escalateHandledUpgradeDays = v; }

    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String v) { this.uploadDir = v; }

    public long getMaxFileSizeMB() { return maxFileSizeMB; }
    public void setMaxFileSizeMB(long v) { this.maxFileSizeMB = v; }

    public double getWeakPointErrorRateThreshold() { return weakPointErrorRateThreshold; }
    public void setWeakPointErrorRateThreshold(double v) { this.weakPointErrorRateThreshold = v; }

    public int getPassScoreThreshold() { return passScoreThreshold; }
    public void setPassScoreThreshold(int v) { this.passScoreThreshold = v; }
}
