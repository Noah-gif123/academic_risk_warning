package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.ScoreTemplate;
import org.apache.ibatis.annotations.Mapper;

/** 成绩构成模板表 数据访问 */
@Mapper
public interface ScoreTemplateMapper extends BaseMapper<ScoreTemplate> {
}
