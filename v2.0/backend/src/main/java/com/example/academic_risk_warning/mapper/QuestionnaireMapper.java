package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.Questionnaire;
import org.apache.ibatis.annotations.Mapper;

/** 问卷表（老师评价 / 知识点讲解反馈） 数据访问 */
@Mapper
public interface QuestionnaireMapper extends BaseMapper<Questionnaire> {
}
