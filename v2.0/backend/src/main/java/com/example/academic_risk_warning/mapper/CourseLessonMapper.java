package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.CourseLesson;
import org.apache.ibatis.annotations.Mapper;

/** 目录课时表（一行=一次课） 数据访问 */
@Mapper
public interface CourseLessonMapper extends BaseMapper<CourseLesson> {
}
