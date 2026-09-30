package com.example.redchina.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.redchina.entity.SysStoryOption;
import com.example.redchina.mapper.SysStoryOptionMapper;
import com.example.redchina.service.SysStoryOptionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
public class SysStoryOptionServiceImpl extends ServiceImpl<SysStoryOptionMapper, SysStoryOption> implements SysStoryOptionService {

    @Resource
    private SysStoryOptionMapper storyOptionMapper;

    @Override
    public List<SysStoryOption> getByNodeId(Long nodeId) {
        return storyOptionMapper.selectByNodeId(nodeId);
    }

    @Override
    public SysStoryOption getByNodeIdAndNextNodeId(Long nodeId, Long nextNodeId) {
        return storyOptionMapper.selectByNodeIdAndNextNodeId(nodeId, nextNodeId);
    }

    @Override
    public List<SysStoryOption> getByStoryId(Long storyId) {
        return storyOptionMapper.selectByStoryId(storyId);
    }

    @Override
    @Transactional
    public boolean removeBatchByNodeIds(List<Long> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return true;
        }
        return storyOptionMapper.deleteBatchByNodeIds(nodeIds) > 0;
    }

    @Override
    @Transactional
    public boolean removeBatchByStoryIds(List<Long> storyIds) {
        if (storyIds == null || storyIds.isEmpty()) {
            return true;
        }
        return storyOptionMapper.deleteBatchByStoryIds(storyIds) > 0;
    }
}
