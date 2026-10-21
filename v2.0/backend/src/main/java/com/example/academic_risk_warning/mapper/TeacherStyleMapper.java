package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.TeacherStyle;
import org.apache.ibatis.annotations.Mapper;

/** 教师尺度Δ表（分项级，含收缩与熔断） 数据访问 */
@Mapper
public interface TeacherStyleMapper extends BaseMapper<TeacherStyle> {
}
