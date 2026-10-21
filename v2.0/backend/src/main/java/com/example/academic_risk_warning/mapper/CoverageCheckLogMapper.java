package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.CoverageCheckLog;
import org.apache.ibatis.annotations.Mapper;

/** 覆盖率体检记录表（去重与督办跟踪） 数据访问 */
@Mapper
public interface CoverageCheckLogMapper extends BaseMapper<CoverageCheckLog> {
}
