package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.CounselorClass;
import org.apache.ibatis.annotations.Mapper;

/** 导员-行政班关联表 数据访问 */
@Mapper
public interface CounselorClassMapper extends BaseMapper<CounselorClass> {
}
