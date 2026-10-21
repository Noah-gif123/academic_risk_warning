package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.ScoreSchemeItem;
import org.apache.ibatis.annotations.Mapper;

/** 评分项定义表（两级；考勤不得入内） 数据访问 */
@Mapper
public interface ScoreSchemeItemMapper extends BaseMapper<ScoreSchemeItem> {
}
