package com.example.redchina.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.redchina.entity.SysStory;
import com.example.redchina.mapper.SysStoryMapper;
import com.example.redchina.service.SysStoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
public class SysStoryServiceImpl extends ServiceImpl<SysStoryMapper, SysStory> implements SysStoryService {

    @Resource
    private SysStoryMapper storyMapper;

    @Override
    public IPage<SysStory> selectStoryPage(IPage<SysStory> page, Long tagId, Integer isRecommend) {
        return storyMapper.selectStoryPage(page, tagId, isRecommend);
    }

    @Override
    public List<SysStory> getByTagIds(List<Long> tagIds) {
        return storyMapper.selectByTagIds(tagIds);
    }

    @Override
    public SysStory getStoryDetail(Long id) {
        return storyMapper.selectStoryDetail(id);
    }

    @Override
    public List<SysStory> getHotStories(Integer limit) {
        return storyMapper.selectHotStories(limit);
    }

    @Override
    @Transactional
    public boolean saveStory(SysStory story) {
        return saveOrUpdate(story);
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        SysStory story = new SysStory();
        story.setId(id);
        story.setStatus(status);
        return updateById(story);
    }
}
