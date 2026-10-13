package com.example.redchina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.redchina.entity.SysStory;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface SysStoryMapper extends BaseMapper<SysStory> {
    // 分页查询剧情（支持标签筛选、推荐筛选）
    IPage<SysStory> selectStoryPage(IPage<SysStory> page,
                                    @Param("tagId") Long tagId,
                                    @Param("isRecommend") Integer isRecommend);

    // 根据标签ID查询剧情（个性化推荐用）
    List<SysStory> selectByTagIds(@Param("tagIds") List<Long> tagIds);

    // 根据剧情ID查询详情（含标签名称）
    SysStory selectStoryDetail(@Param("id") Long id);

    // 查询热门剧情（按解锁量降序）
    List<SysStory> selectHotStories(@Param("limit") Integer limit);

    // 新增：根据状态查询剧情列表
    List<SysStory> selectByStatus(@Param("status") Integer status);

    // 新增：统计标签下的剧情数量
    int countByTagId(@Param("tagId") Long tagId);
}