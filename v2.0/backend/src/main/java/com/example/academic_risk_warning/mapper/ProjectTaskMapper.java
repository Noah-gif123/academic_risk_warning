package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.ProjectTask;
import org.apache.ibatis.annotations.Mapper;

/** 项目任务表 数据访问 */
@Mapper
public interface ProjectTaskMapper extends BaseMapper<ProjectTask> {
}
