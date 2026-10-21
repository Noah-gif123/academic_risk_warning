package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.AdminClassFailRate;
import org.apache.ibatis.annotations.Mapper;

/** 行政班挂科率表（→导员） 数据访问 */
@Mapper
public interface AdminClassFailRateMapper extends BaseMapper<AdminClassFailRate> {
}
