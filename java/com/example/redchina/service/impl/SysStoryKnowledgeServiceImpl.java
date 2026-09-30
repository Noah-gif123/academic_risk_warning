package com.example.redchina.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.example.redchina.entity.SysStoryKnowledge;
import com.example.redchina.mapper.SysStoryKnowledgeMapper;
import com.example.redchina.service.SysStoryKnowledgeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
public class SysStoryKnowledgeServiceImpl extends ServiceImpl<SysStoryKnowledgeMapper, SysStoryKnowledge> implements SysStoryKnowledgeService {

    @Override
    public SysStoryKnowledge getByNodeIdAndLanguage(Long storyNodeId, String languageType) {
        return baseMapper.selectByNodeIdAndLanguage(storyNodeId, languageType);
    }

    @Override
    public List<SysStoryKnowledge> getByStoryId(Long storyId) {
        return baseMapper.selectByStoryId(storyId);
    }

    @Override
    @Transactional
    public boolean saveBatchKnowledge(List<SysStoryKnowledge> knowledgeList) {
        if (knowledgeList == null || knowledgeList.isEmpty()) {
            return true;
        }
        // 批量保存（存在则更新，不存在则新增）
        return saveOrUpdateBatch(knowledgeList);
    }

    @Override
    @Transactional
    public boolean removeByNodeId(Long storyNodeId) {
        LambdaQueryWrapper<SysStoryKnowledge> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysStoryKnowledge::getStoryNodeId, storyNodeId);
        return remove(queryWrapper);
    }
}
