package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.CourseJoinRequest;
import org.apache.ibatis.annotations.Mapper;

/** 入课申请表（学生申请/教师审核/教务知会） 数据访问 */
@Mapper
public interface CourseJoinRequestMapper extends BaseMapper<CourseJoinRequest> {
}
