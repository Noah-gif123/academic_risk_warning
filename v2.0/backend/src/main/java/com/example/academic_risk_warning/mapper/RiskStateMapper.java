package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.RiskState;
import org.apache.ibatis.annotations.Mapper;

/** 当前风险状态表（替代每日快照；一人一课一行） 数据访问 */
@Mapper
public interface RiskStateMapper extends BaseMapper<RiskState> {
}
