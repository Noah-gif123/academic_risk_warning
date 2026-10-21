package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.Student;
import org.apache.ibatis.annotations.Mapper;

/** 学生表 数据访问 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {
}
