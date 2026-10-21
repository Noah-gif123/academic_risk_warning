package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.Course;
import org.apache.ibatis.annotations.Mapper;

/** 课程（科目）表 数据访问 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
