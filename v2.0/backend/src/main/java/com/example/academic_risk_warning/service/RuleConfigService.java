package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.RiskRuleRequest;
import com.example.academic_risk_warning.entity.RiskEngineConfig;
import com.example.academic_risk_warning.mapper.RiskEngineConfigMapper;
import com.example.academic_risk_warning.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 判定规则配置（第二阶段第 5 步）。
 *
 * <p>规则按版本管理：risk_engine_config 全库只有一条 is_active=1；改规则一律<b>新增版本并生效</b>，
 * 老版本保留可回切。判定引擎每次判定都取当前生效版本，并把 engine_version 写进 risk_state，
 * 因此"改了哪一版规则导致等级变化"可以回溯。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleConfigService {

    private final RiskEngineConfigMapper configMapper;
    private final AuditService auditService;

    /** 当前生效版本 + 历史版本（新→旧） */
    public Map<String, Object> list() {
        List<RiskEngineConfig> versions = configMapper.selectList(Wrappers.<RiskEngineConfig>lambdaQuery()
                .orderByDesc(RiskEngineConfig::getId));
        RiskEngineConfig active = versions.stream()
                .filter((c) -> c.getIsActive() != null && c.getIsActive() == 1)
                .findFirst().orElse(null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("active", active);
        map.put("versions", versions.stream().map(this::brief).collect(java.util.stream.Collectors.toList()));
        return map;
    }

    /** 新建版本并立即生效 */
    @Transactional
    public Map<String, Object> create(LoginUser user, RiskRuleRequest req) {
        if (req == null) {
            throw new BizException("缺少规则参数");
        }
        RiskEngineConfig current = activeConfig();
        if (current == null) {
            throw new BizException("没有可用的规则基线，请先初始化 risk_engine_config");
        }
        RiskEngineConfig next = copyOf(current);
        if (req.getThresholdRed() != null) {
            next.setThresholdRed(req.getThresholdRed());
        }
        if (req.getThresholdOrange() != null) {
            next.setThresholdOrange(req.getThresholdOrange());
        }
        if (req.getThresholdYellow() != null) {
            next.setThresholdYellow(req.getThresholdYellow());
        }
        if (req.getDeductAbsent() != null) {
            next.setDeductAbsent(req.getDeductAbsent());
        }
        if (req.getDeductLate() != null) {
            next.setDeductLate(req.getDeductLate());
        }
        if (req.getDeductLeave() != null) {
            next.setDeductLeave(req.getDeductLeave());
        }
        if (req.getLeaveFreeTimes() != null) {
            next.setLeaveFreeTimes(req.getLeaveFreeTimes());
        }
        if (req.getFinalFullScore() != null) {
            next.setFinalFullScore(req.getFinalFullScore());
        }
        if (req.getCoverageThreshold() != null) {
            next.setCoverageThreshold(req.getCoverageThreshold());
        }
        if (req.getConfidenceMinOk() != null) {
            next.setConfidenceMinOk(req.getConfidenceMinOk());
        }
        validate(next);
        String version;
        if (blank(req.getEngineVersion())) {
            version = nextFreeVersion(current.getEngineVersion());
        } else {
            version = req.getEngineVersion().trim();
            if (versionExists(version)) {
                throw new BizException("版本号 " + version + " 已存在，请换一个或留空自动生成");
            }
        }
        next.setId(null);
        next.setEngineVersion(version);
        next.setIsActive(0);
        next.setEffectiveTime(LocalDateTime.now());
        next.setRemark(blank(req.getRemark()) ? "规则调整" : req.getRemark().trim());
        next.setCreateBy(user == null ? null : user.getRefId());
        next.setCreateTime(LocalDateTime.now());
        next.setUpdateBy(user == null ? null : user.getRefId());
        next.setUpdateTime(LocalDateTime.now());
        configMapper.insert(next);
        activate(user, next.getId());
        auditService.record("RISK_ENGINE_CONFIG", next.getId(), "CREATE", null,
                "{\"engineVersion\":\"" + version + "\",\"thresholdRed\":" + next.getThresholdRed()
                        + ",\"thresholdOrange\":" + next.getThresholdOrange()
                        + ",\"thresholdYellow\":" + next.getThresholdYellow() + "}",
                "新增判定规则版本 " + version + "（" + next.getRemark() + "）");
        Map<String, Object> map = list();
        map.put("message", "已切换到新版本 " + version + "，下一次判定起生效");
        return map;
    }

    /** 切换生效版本（回切历史版本也走这里） */
    @Transactional
    public Map<String, Object> activate(LoginUser user, Long id) {
        RiskEngineConfig target = id == null ? null : configMapper.selectById(id);
        if (target == null) {
            throw new BizException("规则版本不存在");
        }
        if (target.getIsActive() != null && target.getIsActive() == 1) {
            throw new BizException("该版本已是当前生效版本");
        }
        LocalDateTime now = LocalDateTime.now();
        configMapper.update(null, Wrappers.<RiskEngineConfig>lambdaUpdate()
                .set(RiskEngineConfig::getIsActive, 0)
                .set(RiskEngineConfig::getUpdateTime, now)
                .eq(RiskEngineConfig::getIsActive, 1));
        target.setIsActive(1);
        target.setEffectiveTime(now);
        target.setUpdateBy(user == null ? null : user.getRefId());
        target.setUpdateTime(now);
        configMapper.updateById(target);
        auditService.record("RISK_ENGINE_CONFIG", target.getId(), "ACTIVATE", null,
                "{\"engineVersion\":\"" + target.getEngineVersion() + "\"}", "切换判定规则版本");
        log.info("判定规则切到 {}（by {}）", target.getEngineVersion(), user == null ? null : user.getLoginNo());
        Map<String, Object> map = list();
        map.put("message", "已切换到 " + target.getEngineVersion() + "，下一次判定起生效");
        return map;
    }

    /** 当前生效版本（供其它服务读取，判定引擎自行查询同一张表） */
    public RiskEngineConfig activeConfig() {
        return configMapper.selectOne(Wrappers.<RiskEngineConfig>lambdaQuery()
                .eq(RiskEngineConfig::getIsActive, 1)
                .orderByDesc(RiskEngineConfig::getId)
                .last("limit 1"));
    }

    // ============================================================ 内部
    private RiskEngineConfig copyOf(RiskEngineConfig src) {
        RiskEngineConfig c = new RiskEngineConfig();
        c.setChannelAEnabled(src.getChannelAEnabled());
        c.setChannelBEnabled(src.getChannelBEnabled());
        c.setChannelCEnabled(src.getChannelCEnabled());
        c.setSignalWeightsJson(src.getSignalWeightsJson());
        c.setDeltaEnabled(src.getDeltaEnabled());
        c.setCourseFactorEnabled(src.getCourseFactorEnabled());
        c.setThresholdRed(src.getThresholdRed());
        c.setThresholdOrange(src.getThresholdOrange());
        c.setThresholdYellow(src.getThresholdYellow());
        c.setCoverageThreshold(src.getCoverageThreshold());
        c.setCoverageSoftWeeks(src.getCoverageSoftWeeks());
        c.setCounselorWeekStart(src.getCounselorWeekStart());
        c.setCounselorRateL1(src.getCounselorRateL1());
        c.setCounselorMinL1(src.getCounselorMinL1());
        c.setCounselorRateL2(src.getCounselorRateL2());
        c.setCounselorMinL2(src.getCounselorMinL2());
        c.setCounselorRiskFloor(src.getCounselorRiskFloor());
        c.setMultiCourseThreshold(src.getMultiCourseThreshold());
        c.setInvalidCheckDays(src.getInvalidCheckDays());
        c.setDeductAbsent(src.getDeductAbsent());
        c.setDeductLate(src.getDeductLate());
        c.setDeductLeave(src.getDeductLeave());
        c.setLeaveFreeTimes(src.getLeaveFreeTimes());
        c.setFinalFullScore(src.getFinalFullScore());
        c.setDeltaShrinkK(src.getDeltaShrinkK());
        c.setDeltaMinSample(src.getDeltaMinSample());
        c.setDeltaFuseThreshold(src.getDeltaFuseThreshold());
        c.setConfidenceMinOk(src.getConfidenceMinOk());
        return c;
    }

    private void validate(RiskEngineConfig c) {
        BigDecimal red = c.getThresholdRed();
        BigDecimal orange = c.getThresholdOrange();
        BigDecimal yellow = c.getThresholdYellow();
        if (red == null || orange == null || yellow == null) {
            throw new BizException("三档阈值必须齐全");
        }
        if (red.compareTo(orange) <= 0 || orange.compareTo(yellow) <= 0) {
            throw new BizException("阈值必须满足 红 > 橙 > 黄（当前：" + red + " / " + orange + " / " + yellow + "）");
        }
        if (yellow.signum() <= 0) {
            throw new BizException("黄色阈值必须大于 0");
        }
        if (c.getDeductAbsent() != null && c.getDeductAbsent().signum() < 0
                || c.getDeductLate() != null && c.getDeductLate().signum() < 0
                || c.getDeductLeave() != null && c.getDeductLeave().signum() < 0) {
            throw new BizException("扣分系数不能为负数");
        }
        if (c.getLeaveFreeTimes() != null && c.getLeaveFreeTimes() < 0) {
            throw new BizException("请假免扣次数不能为负数");
        }
        if (c.getFinalFullScore() == null || c.getFinalFullScore().signum() <= 0) {
            throw new BizException("期末满分必须大于 0");
        }
        if (c.getCoverageThreshold() != null
                && (c.getCoverageThreshold().signum() <= 0
                || c.getCoverageThreshold().compareTo(BigDecimal.ONE) > 0)) {
            throw new BizException("覆盖率阈值应在 0~1 之间");
        }
        if (c.getConfidenceMinOk() != null
                && (c.getConfidenceMinOk().signum() < 0 || c.getConfidenceMinOk().compareTo(BigDecimal.ONE) > 0)) {
            throw new BizException("置信度下限应在 0~1 之间");
        }
    }

    /** 版本号 +0.1.0：v2.0.0 → v2.1.0 */
    private String bumpVersion(String version) {        if (version == null || version.isEmpty()) {
            return "v2.1.0";
        }
        String v = version.trim();
        String prefix = v.startsWith("v") || v.startsWith("V") ? v.substring(0, 1) : "";
        String body = prefix.isEmpty() ? v : v.substring(1);
        String[] parts = body.split("\\.");
        try {
            int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 2;
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return prefix + major + "." + (minor + 1) + ".0";
        } catch (NumberFormatException e) {
            return prefix + body + ".1";
        }
    }

    /** 自动生成未被占用的版本号：v2.0.0 → v2.1.0，已被占用则继续 +0.1.0 */
    private String nextFreeVersion(String base) {
        String candidate = bumpVersion(base);
        for (int i = 0; i < 50 && versionExists(candidate); i++) {
            candidate = bumpVersion(candidate);
        }
        return candidate;
    }

    private boolean versionExists(String version) {
        return configMapper.selectCount(Wrappers.<RiskEngineConfig>lambdaQuery()
                .eq(RiskEngineConfig::getEngineVersion, version)) > 0;
    }

    private Map<String, Object> brief(RiskEngineConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("engineVersion", c.getEngineVersion());
        m.put("isActive", c.getIsActive());
        m.put("effectiveTime", c.getEffectiveTime());
        m.put("thresholdRed", c.getThresholdRed());
        m.put("thresholdOrange", c.getThresholdOrange());
        m.put("thresholdYellow", c.getThresholdYellow());
        m.put("deductAbsent", c.getDeductAbsent());
        m.put("deductLate", c.getDeductLate());
        m.put("deductLeave", c.getDeductLeave());
        m.put("leaveFreeTimes", c.getLeaveFreeTimes());
        m.put("finalFullScore", c.getFinalFullScore());
        m.put("coverageThreshold", c.getCoverageThreshold());
        m.put("confidenceMinOk", c.getConfidenceMinOk());
        m.put("remark", c.getRemark());
        m.put("createTime", c.getCreateTime());
        return m;
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
