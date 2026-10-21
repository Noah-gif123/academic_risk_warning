package com.example.academic_risk_warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.academic_risk_warning.entity.LessonResource;
import org.apache.ibatis.annotations.Mapper;

/** 课时资源与任务表（课前/课中/课后） 数据访问 */
@Mapper
public interface LessonResourceMapper extends BaseMapper<LessonResource> {
}
