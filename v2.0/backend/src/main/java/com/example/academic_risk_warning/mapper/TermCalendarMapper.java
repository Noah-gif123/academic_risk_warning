package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.TermCalendar;
import org.apache.ibatis.annotations.Mapper;

/** 教学周历表（周次/假期/考试周） 数据访问 */
@Mapper
public interface TermCalendarMapper extends BaseMapper<TermCalendar> {
}
