package com.example.redchina.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.redchina.entity.SysStory;
import java.util.List;

public interface SysStoryService extends IService<SysStory> {
    // 分页查询剧情（支持标签、推荐筛选）
    IPage<SysStory> selectStoryPage(IPage<SysStory> page, Long tagId, Integer isRecommend);

    // 根据标签ID列表查询剧情
    List<SysStory> getByTagIds(List<Long> tagIds);

    // 根据ID查询剧情详情（含标签）
    SysStory getStoryDetail(Long id);

    // 查询热门剧情
    List<SysStory> getHotStories(Integer limit);

    // 管理员新增/编辑剧情
    boolean saveStory(SysStory story);

    // 管理员更新剧情状态（上架/下架）
    boolean updateStatus(Long id, Integer status);
}
