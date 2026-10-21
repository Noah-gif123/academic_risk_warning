package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.SysAccount;
import org.apache.ibatis.annotations.Mapper;

/** 统一账号表（四端登录） 数据访问 */
@Mapper
public interface SysAccountMapper extends BaseMapper<SysAccount> {
}
