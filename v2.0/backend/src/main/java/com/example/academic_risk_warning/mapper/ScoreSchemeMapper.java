package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.ScoreScheme;
import org.apache.ibatis.annotations.Mapper;

/** 成绩构成表（课程组共用，锁定后不可改） 数据访问 */
@Mapper
public interface ScoreSchemeMapper extends BaseMapper<ScoreScheme> {
}
