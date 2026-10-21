package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/** 通知主体表（聚合与业务关联） 数据访问 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
