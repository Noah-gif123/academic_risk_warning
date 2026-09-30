package com.example.redchina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.redchina.entity.SysUserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SysUserBehaviorMapper extends BaseMapper<SysUserBehavior> {
    // 根据用户ID和目标类型查询行为记录
    List<SysUserBehavior> selectByUserIdAndTargetType(@Param("userId") Long userId, @Param("targetType") String targetType);

    // 统计用户某类目标的互动频次（用于推荐）
    List<String>UserTargetFrequency(@Param("userId") Long userId, @Param("targetType") String targetType);
}
