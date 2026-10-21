package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.TeachingClassFailRate;
import org.apache.ibatis.annotations.Mapper;

/** 教学班挂科率表（→老师） 数据访问 */
@Mapper
public interface TeachingClassFailRateMapper extends BaseMapper<TeachingClassFailRate> {
}
