package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.AttendanceArchive;
import org.apache.ibatis.annotations.Mapper;

/** 考勤归档批次表（期中/期末，双轨制的落点） 数据访问 */
@Mapper
public interface AttendanceArchiveMapper extends BaseMapper<AttendanceArchive> {
}
