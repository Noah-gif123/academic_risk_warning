package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.ProjectSubmission;
import org.apache.ibatis.annotations.Mapper;

/** 项目提交与批阅表 数据访问 */
@Mapper
public interface ProjectSubmissionMapper extends BaseMapper<ProjectSubmission> {
}
