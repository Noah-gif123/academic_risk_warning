package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/** 统一审计日志表 数据访问 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
