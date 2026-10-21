package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

/** 教务/管理员表 数据访问 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}
