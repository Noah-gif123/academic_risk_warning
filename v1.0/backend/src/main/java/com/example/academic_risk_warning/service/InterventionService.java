package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.config.WarningSystemProperties;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.mapper.AlertSnapshotMapper;
import com.example.academic_risk_warning.mapper.InterventionRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 干预措施服务
 * 负责干预记录的增删改查 + 效果回测
 */
@Service
public class InterventionService {

    private final InterventionRecordMapper interventionRecordMapper;
    private final AlertSnapshotMapper alertSnapshotMapper;
    private final WarningSystemProperties warningProperties;

    public InterventionService(InterventionRecordMapper interventionRecordMapper,
                               AlertSnapshotMapper alertSnapshotMapper,
                               WarningSystemProperties warningProperties) {
        this.interventionRecordMapper = interventionRecordMapper;
        this.alertSnapshotMapper = alertSnapshotMapper;
        this.warningProperties = warningProperties;
    }

    /**
     * 创建干预记录
     */
    @Transactional
    public InterventionRecord create(Long alertId, Long studentId, Long teacherId,
                                     String interventionType, String description,
                                     BigDecimal riskScoreBefore) {
        InterventionRecord record = new InterventionRecord();
        record.setAlertId(alertId);
        record.setStudentId(studentId);
        record.setTeacherId(teacherId);
        record.setInterventionType(interventionType);
        record.setDescription(description);
        record.setStatus("EXECUTING");
        record.setRiskScoreBefore(riskScoreBefore);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        interventionRecordMapper.insert(record);
        return record;
    }

    /**
     * 更新干预状态
     */
    public boolean updateStatus(Long interventionId, String status, String resultNote) {
        InterventionRecord record = interventionRecordMapper.selectById(interventionId);
        if (record == null) return false;
        record.setStatus(status);
        record.setResultNote(resultNote);
        record.setUpdateTime(LocalDateTime.now());
        return interventionRecordMapper.updateById(record) > 0;
    }

    /**
     * 效果回测: 对比干预前后最近快照的风险分变化
     * 负数 = 改善, 正数 = 恶化, 0 = 无变化
     */
    public BigDecimal checkEffect(Long interventionId) {
        InterventionRecord record = interventionRecordMapper.selectById(interventionId);
        if (record == null) return null;

        // 查最新快照
        List<AlertSnapshot> snapshots = alertSnapshotMapper.selectList(
                new LambdaQueryWrapper<AlertSnapshot>()
                        .eq(AlertSnapshot::getStudentId, record.getStudentId())
                        .eq(AlertSnapshot::getCourseId, getCourseIdFromAlert(record.getAlertId()))
                        .orderByDesc(AlertSnapshot::getSnapshotDate)
                        .last("LIMIT 1")
        );

        if (snapshots.isEmpty()) return BigDecimal.ZERO;

        AlertSnapshot latest = snapshots.get(0);
        BigDecimal currentRisk = latest.getRiskScore();
        BigDecimal change = currentRisk != null && record.getRiskScoreBefore() != null
                ? currentRisk.subtract(record.getRiskScoreBefore()).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 回写
        record.setRiskScoreAfter(currentRisk);
        record.setRiskScoreChange(change);
        record.setEffectCheckTime(LocalDateTime.now());

        // 自动判定: 风险分下降>=阈值且当前风险分低于低风险阈值 → COMPLETED
        if (change.doubleValue() <= warningProperties.getInterventionImproveThreshold()
                && currentRisk.doubleValue() < warningProperties.getInterventionLowRiskThreshold()) {
            record.setStatus("COMPLETED");
            record.setResultNote("干预有效，学生风险显著降低");
        } else if (change.doubleValue() < 0) {
            record.setResultNote("干预后有改善趋势，需继续跟进");
        } else {
            record.setStatus("INEFFECTIVE");
            record.setResultNote("干预暂未见效，建议调整干预策略");
        }

        record.setUpdateTime(LocalDateTime.now());
        interventionRecordMapper.updateById(record);

        return change;
    }

    /**
     * 获取某预警的所有干预记录
     */
    public List<InterventionRecord> getByAlertId(Long alertId) {
        return interventionRecordMapper.selectList(
                new LambdaQueryWrapper<InterventionRecord>()
                        .eq(InterventionRecord::getAlertId, alertId)
                        .orderByDesc(InterventionRecord::getCreateTime)
        );
    }

    /**
     * 获取某学生的所有干预记录
     */
    public List<InterventionRecord> getByStudentId(Long studentId) {
        return interventionRecordMapper.selectList(
                new LambdaQueryWrapper<InterventionRecord>()
                        .eq(InterventionRecord::getStudentId, studentId)
                        .orderByDesc(InterventionRecord::getCreateTime)
        );
    }

    private Long getCourseIdFromAlert(Long alertId) {
        // 从 intervention_record 关联的 alert_record 获取 course_id
        // 简化: 通过 alert_snapshot 反查
        List<AlertSnapshot> snapshots = alertSnapshotMapper.selectList(
                new LambdaQueryWrapper<AlertSnapshot>()
                        .eq(AlertSnapshot::getRelatedAlertId, alertId)
                        .last("LIMIT 1")
        );
        return snapshots.isEmpty() ? null : snapshots.get(0).getCourseId();
    }
}
