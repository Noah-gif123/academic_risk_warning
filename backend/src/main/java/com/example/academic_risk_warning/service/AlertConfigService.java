package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.entity.AlertRuleConfig;
import com.example.academic_risk_warning.mapper.AlertRuleConfigMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 预警规则配置 CRUD 服务
 */
@Service
public class AlertConfigService {

    private final AlertRuleConfigMapper mapper;
    private final WarningGenerationService warningGenerationService;

    public AlertConfigService(AlertRuleConfigMapper mapper,
                              WarningGenerationService warningGenerationService) {
        this.mapper = mapper;
        this.warningGenerationService = warningGenerationService;
    }

    /** 获取某体系的所有配置 */
    public List<AlertRuleConfig> listBySystem(String systemType) {
        return mapper.selectList(new LambdaQueryWrapper<AlertRuleConfig>()
                .eq(AlertRuleConfig::getSystemType, systemType)
                .orderByAsc(AlertRuleConfig::getCourseId));
    }

    /** 获取所有配置 */
    public List<AlertRuleConfig> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<AlertRuleConfig>()
                .orderByAsc(AlertRuleConfig::getSystemType)
                .orderByAsc(AlertRuleConfig::getCourseId));
    }

    /** 根据ID获取 */
    public AlertRuleConfig getById(Long id) {
        return mapper.selectById(id);
    }

    /** 创建或更新配置 */
    public AlertRuleConfig save(AlertRuleConfig config) {
        if (config.getId() != null) {
            mapper.updateById(config);
        } else {
            mapper.insert(config);
        }
        invalidateCache(config.getSystemType());
        return config;
    }

    /** 删除配置 */
    public void delete(Long id) {
        AlertRuleConfig config = mapper.selectById(id);
        if (config != null) {
            mapper.deleteById(id);
            invalidateCache(config.getSystemType());
        }
    }

    /** 使缓存失效（让 WarningGenerationService 下次重新加载DB配置） */
    private void invalidateCache(String systemType) {
        warningGenerationService.invalidateRuleCache(systemType);
    }
}
