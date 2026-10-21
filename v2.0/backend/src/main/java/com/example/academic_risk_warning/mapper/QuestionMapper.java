package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.Question;
import org.apache.ibatis.annotations.Mapper;

/** 题目表 数据访问 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
