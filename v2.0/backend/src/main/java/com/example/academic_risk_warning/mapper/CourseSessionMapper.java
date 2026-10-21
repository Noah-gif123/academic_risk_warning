package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.CourseSession;
import org.apache.ibatis.annotations.Mapper;

/** 课次表（一次课一行；点名与覆盖率的基础） 数据访问 */
@Mapper
public interface CourseSessionMapper extends BaseMapper<CourseSession> {
}
