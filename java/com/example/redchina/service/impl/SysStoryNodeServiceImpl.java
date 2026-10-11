package com.example.redchina.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.redchina.entity.SysStoryNode;
import com.example.redchina.mapper.SysStoryNodeMapper;
import com.example.redchina.service.SysStoryNodeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
public class SysStoryNodeServiceImpl extends ServiceImpl<SysStoryNodeMapper, SysStoryNode> implements SysStoryNodeService {

    @Resource
    private SysStoryNodeMapper storyNodeMapper;

    @Override
    public List<SysStoryNode> getByStoryId(Long storyId) {
        return storyNodeMapper.selectByStoryId(storyId);
    }

    @Override
    public List<SysStoryNode> getByParentNodeId(Long parentNodeId) {
        return storyNodeMapper.selectByParentNodeId(parentNodeId);
    }

    @Override
    public SysStoryNode getNodeDetail(Long id) {
        return storyNodeMapper.selectNodeDetail(id);
    }

    @Override
    public List<SysStoryNode> getEndNodesByStoryId(Long storyId) {
        return storyNodeMapper.selectEndNodesByStoryId(storyId);
    }

    @Override
    @Transactional
    public boolean removeBatchByStoryIds(List<Long> storyIds) {
        if (storyIds == null || storyIds.isEmpty()) {
            return true;
        }
        return storyNodeMapper.deleteBatchByStoryIds(storyIds) > 0;
    }


    @Override
    public SysStoryNode getInitialNode(Long storyId) {
        // 调用Mapper层方法查询初始节点（parent_node_id为0或null的节点）
        return storyNodeMapper.selectInitialNode(storyId);
    }
}
