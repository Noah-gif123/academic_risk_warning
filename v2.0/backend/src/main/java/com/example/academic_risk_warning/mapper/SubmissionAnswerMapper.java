package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.SubmissionAnswer;
import org.apache.ibatis.annotations.Mapper;

/** 逐题作答与判分表 数据访问 */
@Mapper
public interface SubmissionAnswerMapper extends BaseMapper<SubmissionAnswer> {
}
