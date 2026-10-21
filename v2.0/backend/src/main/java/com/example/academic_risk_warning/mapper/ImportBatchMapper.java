package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.ImportBatch;
import org.apache.ibatis.annotations.Mapper;

/** 导入批次表（可查可回滚） 数据访问 */
@Mapper
public interface ImportBatchMapper extends BaseMapper<ImportBatch> {
}
