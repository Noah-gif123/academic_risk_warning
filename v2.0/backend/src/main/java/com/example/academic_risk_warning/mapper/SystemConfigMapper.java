package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;

/** 运营配置表 数据访问 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {
}
