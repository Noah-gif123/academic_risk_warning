package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.Term;
import org.apache.ibatis.annotations.Mapper;

/** 学期表（全库时间轴） 数据访问 */
@Mapper
public interface TermMapper extends BaseMapper<Term> {
}
