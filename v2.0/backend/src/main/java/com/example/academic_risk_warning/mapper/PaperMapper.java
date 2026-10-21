package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.Paper;
import org.apache.ibatis.annotations.Mapper;

/** 试卷表（测验/作业/考试共用） 数据访问 */
@Mapper
public interface PaperMapper extends BaseMapper<Paper> {
}
