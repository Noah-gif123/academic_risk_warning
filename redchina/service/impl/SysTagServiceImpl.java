package com.example.redchina.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.example.redchina.entity.SysCulturalTerm;
import com.example.redchina.entity.SysStory;
import com.example.redchina.entity.SysTag;
import com.example.redchina.entity.SysUserTag;
import com.example.redchina.mapper.SysCulturalTermMapper;
import com.example.redchina.mapper.SysStoryMapper;
import com.example.redchina.mapper.SysTagMapper;
import com.example.redchina.mapper.SysUserTagMapper;
import com.example.redchina.service.SysTagService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
public class SysTagServiceImpl extends ServiceImpl<SysTagMapper, SysTag> implements SysTagService {

    @Resource
    private SysStoryMapper storyMapper;
    @Resource
    private SysUserTagMapper userTagMapper;
    @Resource
    private SysCulturalTermMapper termMapper;

    @Override
    public IPage<SysTag> selectTagPage(IPage<SysTag> page, String tagType, String keyword) {
        LambdaQueryWrapper<SysTag> queryWrapper = new LambdaQueryWrapper<>();
        if (tagType != null && !tagType.isEmpty()) {
            queryWrapper.eq(SysTag::getTagType, tagType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(SysTag::getTagName, keyword);
        }
        return page(page, queryWrapper);
    }

    @Override
    public List<SysTag> selectByTagType(String tagType) {
        LambdaQueryWrapper<SysTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysTag::getTagType, tagType);
        return list(queryWrapper);
    }

    @Override
    public boolean checkTagNameUnique(String tagName, Long id) {
        LambdaQueryWrapper<SysTag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysTag::getTagName, tagName);
        if (id != null) {
            queryWrapper.ne(SysTag::getId, id);
        }
        return count(queryWrapper) == 0;
    }

    @Override
    @Transactional
    public boolean removeTagById(Long id) {
        // 1. 校验是否有关联剧情
        LambdaQueryWrapper<SysStory> storyQuery = new LambdaQueryWrapper<>();
        storyQuery.eq(SysStory::getTagId, id);
        if (storyMapper.selectCount(storyQuery) > 0) {
            throw new RuntimeException("该标签已关联剧情，无法删除");
        }

        // 2. 校验是否有关联术语
        LambdaQueryWrapper<SysCulturalTerm> termQuery = new LambdaQueryWrapper<>();
        termQuery.eq(SysCulturalTerm::getTagId, id);
        if (termMapper.selectCount(termQuery) > 0) {
            throw new RuntimeException("该标签已关联文化术语，无法删除");
        }

        // 3. 删除用户-标签关联
        LambdaQueryWrapper<SysUserTag> userTagQuery = new LambdaQueryWrapper<>();
        userTagQuery.eq(SysUserTag::getTagId, id);
        userTagMapper.delete(userTagQuery);

        // 4. 删除标签
        return removeById(id);
    }
}
