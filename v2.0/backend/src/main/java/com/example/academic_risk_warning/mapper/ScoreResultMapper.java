package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.ScoreResult;
import org.apache.ibatis.annotations.Mapper;

/** 最终成绩单（判定结果留档；折算分不落库） 数据访问 */
@Mapper
public interface ScoreResultMapper extends BaseMapper<ScoreResult> {
}
