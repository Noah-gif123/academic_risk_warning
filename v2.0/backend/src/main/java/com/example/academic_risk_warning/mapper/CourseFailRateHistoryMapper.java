package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.CourseFailRateHistory;
import org.apache.ibatis.annotations.Mapper;

/** 课程挂科率历史表（→课程因子 c） 数据访问 */
@Mapper
public interface CourseFailRateHistoryMapper extends BaseMapper<CourseFailRateHistory> {
}
