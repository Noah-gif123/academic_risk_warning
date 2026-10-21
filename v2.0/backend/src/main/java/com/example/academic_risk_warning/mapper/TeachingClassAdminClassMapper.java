package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.TeachingClassAdminClass;
import org.apache.ibatis.annotations.Mapper;

/** 教学班-行政班关联表 数据访问 */
@Mapper
public interface TeachingClassAdminClassMapper extends BaseMapper<TeachingClassAdminClass> {
}
