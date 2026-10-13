package com.example.redchina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.redchina.entity.SysUserDesign;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SysUserDesignMapper extends BaseMapper<SysUserDesign> {
    // 根据用户ID查询设计列表
    List<SysUserDesign> selectByUserId(@Param("userId") Long userId);

    // 根据标签查询设计列表（用于推荐）
    List<SysUserDesign> selectByTags(@Param("tag") String tag);
}
