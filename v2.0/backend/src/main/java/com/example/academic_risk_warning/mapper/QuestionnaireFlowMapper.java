package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.QuestionnaireFlow;
import org.apache.ibatis.annotations.Mapper;

/** 问卷流转状态表（防 4 环节断链） 数据访问 */
@Mapper
public interface QuestionnaireFlowMapper extends BaseMapper<QuestionnaireFlow> {
}
