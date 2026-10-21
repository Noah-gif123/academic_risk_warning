package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.AttendanceRecordLog;
import org.apache.ibatis.annotations.Mapper;

/** 考勤改动留痕表（行级 before/after） 数据访问 */
@Mapper
public interface AttendanceRecordLogMapper extends BaseMapper<AttendanceRecordLog> {
}
