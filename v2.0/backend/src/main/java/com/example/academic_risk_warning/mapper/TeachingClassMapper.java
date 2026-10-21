package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.TeachingClass;
import org.apache.ibatis.annotations.Mapper;

/** 教学班表（1 位老师） 数据访问 */
@Mapper
public interface TeachingClassMapper extends BaseMapper<TeachingClass> {
}
