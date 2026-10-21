package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.QuestionnaireAnswer;
import org.apache.ibatis.annotations.Mapper;

/** 问卷作答明细表（匿名去重） 数据访问 */
@Mapper
public interface QuestionnaireAnswerMapper extends BaseMapper<QuestionnaireAnswer> {
}
