package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.ProjectContribution;
import org.apache.ibatis.annotations.Mapper;

/** 团队贡献表（组长提交，不可修改） 数据访问 */
@Mapper
public interface ProjectContributionMapper extends BaseMapper<ProjectContribution> {
}
