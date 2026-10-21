package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.Enrollment;
import org.apache.ibatis.annotations.Mapper;

/** 选课表（学生×教学班×学期） 数据访问 */
@Mapper
public interface EnrollmentMapper extends BaseMapper<Enrollment> {
}
