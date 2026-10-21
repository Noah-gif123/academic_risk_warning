package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.RiskEngineConfig;
import org.apache.ibatis.annotations.Mapper;

/** 风险引擎配置表（版本化） 数据访问 */
@Mapper
public interface RiskEngineConfigMapper extends BaseMapper<RiskEngineConfig> {
}
