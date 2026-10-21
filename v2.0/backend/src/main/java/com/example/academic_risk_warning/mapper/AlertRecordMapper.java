package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.AlertRecord;
import org.apache.ibatis.annotations.Mapper;

/** 预警记录表（重建：带通道/置信度/引擎版本） 数据访问 */
@Mapper
public interface AlertRecordMapper extends BaseMapper<AlertRecord> {
}
