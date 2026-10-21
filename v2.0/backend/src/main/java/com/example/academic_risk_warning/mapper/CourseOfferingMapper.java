package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.CourseOffering;
import org.apache.ibatis.annotations.Mapper;

/** 开课实例表（计划/实际课次、及格线） 数据访问 */
@Mapper
public interface CourseOfferingMapper extends BaseMapper<CourseOffering> {
}
