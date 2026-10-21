package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.AlertFlowLog;
import org.apache.ibatis.annotations.Mapper;

/** 预警生命周期时间线（替代快照曲线） 数据访问 */
@Mapper
public interface AlertFlowLogMapper extends BaseMapper<AlertFlowLog> {
}
