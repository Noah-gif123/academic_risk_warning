package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.Submission;
import org.apache.ibatis.annotations.Mapper;

/** 提交与作答记录表 数据访问 */
@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {
}
