package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.ExamHeadcount;
import org.apache.ibatis.annotations.Mapper;

/** 实际考核人数确认与成绩录入核对表 数据访问 */
@Mapper
public interface ExamHeadcountMapper extends BaseMapper<ExamHeadcount> {
}
