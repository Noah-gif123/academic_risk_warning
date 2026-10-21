package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.Major;
import org.apache.ibatis.annotations.Mapper;

/** 专业/学院表 数据访问 */
@Mapper
public interface MajorMapper extends BaseMapper<Major> {
}
