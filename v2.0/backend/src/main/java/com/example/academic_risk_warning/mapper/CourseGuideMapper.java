package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.CourseGuide;
import org.apache.ibatis.annotations.Mapper;

/** 课程导学（每位教师一份） 数据访问 */
@Mapper
public interface CourseGuideMapper extends BaseMapper<CourseGuide> {
}
