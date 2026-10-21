package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.AttendanceRecord;
import org.apache.ibatis.annotations.Mapper;

/** 考勤明细表（学生×课次，双轨：平时/归档） 数据访问 */
@Mapper
public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecord> {
}
