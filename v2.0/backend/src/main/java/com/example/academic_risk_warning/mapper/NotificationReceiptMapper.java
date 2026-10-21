package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.NotificationReceipt;
import org.apache.ibatis.annotations.Mapper;

/** 通知投递与回执表（每个接收人一行） 数据访问 */
@Mapper
public interface NotificationReceiptMapper extends BaseMapper<NotificationReceipt> {
}
