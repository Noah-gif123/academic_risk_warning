package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.ScoreItem;
import org.apache.ibatis.annotations.Mapper;

/** 分项得分明细表（分项级 Δ 的数据基础） 数据访问 */
@Mapper
public interface ScoreItemMapper extends BaseMapper<ScoreItem> {
}
