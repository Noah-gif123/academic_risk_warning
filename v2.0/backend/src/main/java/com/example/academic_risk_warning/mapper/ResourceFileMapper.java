package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.ResourceFile;
import org.apache.ibatis.annotations.Mapper;

/** 资源文件表（课程资料库） 数据访问 */
@Mapper
public interface ResourceFileMapper extends BaseMapper<ResourceFile> {
}
