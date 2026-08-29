package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.AlertRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 预警记录 Mapper
 */
@Mapper
public interface AlertRecordMapper extends BaseMapper<AlertRecord> {

    /**
     * 批量插入预警记录
     */
    int insertBatch(@Param("list") List<AlertRecord> list);
}
