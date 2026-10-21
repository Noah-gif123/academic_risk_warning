package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.ClassAlertNotification;
import org.apache.ibatis.annotations.Mapper;

/** 班集体提醒去重表（导员端，防天天提醒） 数据访问 */
@Mapper
public interface ClassAlertNotificationMapper extends BaseMapper<ClassAlertNotification> {
}
