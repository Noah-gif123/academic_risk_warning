package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.CourseGroup;
import org.apache.ibatis.annotations.Mapper;

/** 课程组表（课程×学期，无组长） 数据访问 */
@Mapper
public interface CourseGroupMapper extends BaseMapper<CourseGroup> {
}
