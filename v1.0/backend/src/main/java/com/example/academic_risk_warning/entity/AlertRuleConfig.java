package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警规则配置表 (v3: 全维度可配置)
 * 所有阈值/权重均可通过管理端界面动态调节，不再硬编码。
 */
@Data
@TableName("alert_rule_config")
public class AlertRuleConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 体系: FRESHMAN-新生, SENIOR-老生 */
    private String systemType;

    /** 课程ID(NULL=默认规则) */
    private Long courseId;

    /** 学业风险权重 */
    private BigDecimal weightAcademic;

    /** 作业风险权重 */
    private BigDecimal weightHomework;

    /** 知识点风险权重 */
    private BigDecimal weightKnowledge;

    /** 出勤风险权重 */
    private BigDecimal weightAttendance;

    /** 历史风险权重 */
    private BigDecimal weightHistory;

    /** 红色预警总分阈值 */
    private BigDecimal thresholdRed;

    /** 橙色预警总分阈值 */
    private BigDecimal thresholdOrange;

    /** 黄色预警总分阈值 */
    private BigDecimal thresholdYellow;

    /** 作业提交率阈值 */
    private BigDecimal homeworkSubmitRateThreshold;

    /** 知识点正确率阈值 */
    private BigDecimal knowledgeCorrectRateThreshold;

    /** 出勤率阈值 */
    private BigDecimal attendanceRateThreshold;

    /** 成绩骤降阈值 */
    private BigDecimal scoreDropThreshold;

    /** 无数据时学业默认风险分 */
    private BigDecimal defaultAcademicRisk;

    /** 无数据时作业默认风险分 */
    private BigDecimal defaultHomeworkRisk;

    /** 无数据时出勤默认风险分 */
    private BigDecimal defaultAttendanceRisk;

    /** 无数据时知识点默认风险分 */
    private BigDecimal defaultKnowledgeRisk;

    /** 老生无历史数据默认风险分 */
    private BigDecimal defaultHistoryRisk;

    /** 新生历史风险默认分 */
    private BigDecimal defaultHistoryFreshmanRisk;

    // ==================== v3 新增: 各维度内部子权重 ====================

    /** 作业维度-提交率子权重(默认40) */
    private BigDecimal homeworkSubmitWeight;

    /** 作业维度-按时提交子权重(默认30) */
    private BigDecimal homeworkOntimeWeight;

    /** 作业维度-均分子权重(默认30) */
    private BigDecimal homeworkAvgScoreWeight;

    /** 出勤维度-缺勤子权重(默认50) */
    private BigDecimal attendanceAbsentWeight;

    /** 出勤维度-迟到子权重(默认30) */
    private BigDecimal attendanceLateWeight;

    /** 出勤维度-随堂测验子权重(默认20) */
    private BigDecimal attendanceQuizWeight;

    /** 知识点维度-正确率子权重(默认50) */
    private BigDecimal knowledgeCorrectWeight;

    /** 知识点维度-薄弱知识点占比子权重(默认30) */
    private BigDecimal knowledgeWeakRateWeight;

    /** 知识点维度-基础正确率子权重(默认20) */
    private BigDecimal knowledgeBasicRateWeight;

    // ==================== v3 新增: 历史风险加减分 ====================

    /** 上学期挂科加分(默认15) */
    private BigDecimal historyFailedBonus;

    /** 学习不稳定加分(默认20) */
    private BigDecimal historyUnstableBonus;

    /** 学习一般加分(默认10) */
    private BigDecimal historyNormalBonus;

    // ==================== v3 新增: 学习时长风险参数 ====================

    /** 学习时长回溯周数(默认4) */
    private Integer studyLookbackWeeks;

    /** 学习时长下滑判定比例(默认0.5, 最新周/4周前 < 此值判为下滑) */
    private BigDecimal studyDeclineRatio;

    /** 学习时长下滑加分(默认10) */
    private BigDecimal studyDeclineBonus;

    /** 学习时长无数据默认风险分(默认10) */
    private BigDecimal studyDefaultRisk;

    // ==================== v3 新增: 预测成绩参数 ====================

    /** 预测成绩及格线(默认60) */
    private BigDecimal predictedPassScore;

    /** 预测成绩-作业提交率扣分阈值(默认70) */
    private BigDecimal predictedSubmitThreshold;

    /** 预测成绩-出勤率扣分阈值(默认80) */
    private BigDecimal predictedAttendanceThreshold;

    /** 是否启用 */
    private Boolean isActive;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
