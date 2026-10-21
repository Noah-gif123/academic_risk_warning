package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.InterventionRecord;
import org.apache.ibatis.annotations.Mapper;

/** 干预记录表（重构：支持教师/导员/教务三级与上报闭环） 数据访问 */
@Mapper
public interface InterventionRecordMapper extends BaseMapper<InterventionRecord> {
}
