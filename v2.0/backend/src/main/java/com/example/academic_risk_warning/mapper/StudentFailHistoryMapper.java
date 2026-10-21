package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.StudentFailHistory;
import org.apache.ibatis.annotations.Mapper;

/** 学生跨学期成绩/挂科历史表 数据访问 */
@Mapper
public interface StudentFailHistoryMapper extends BaseMapper<StudentFailHistory> {
}
