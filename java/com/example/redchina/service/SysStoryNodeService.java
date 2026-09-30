package com.example.redchina.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.redchina.entity.SysStoryNode;

import java.util.List;

public interface SysStoryNodeService extends IService<SysStoryNode> {
    // 根据剧情ID查询所有节点
    List<SysStoryNode> getByStoryId(Long storyId);

    // 根据父节点ID查询子节点
    List<SysStoryNode> getByParentNodeId(Long parentNodeId);

    // 根据节点ID查询节点详情（含关联知识点ID）
    SysStoryNode getNodeDetail(Long id);

    // 查询剧情的所有结局节点
    List<SysStoryNode> getEndNodesByStoryId(Long storyId);

    // 批量删除剧情对应的节点
    boolean removeBatchByStoryIds(List<Long> storyIds);


    SysStoryNode getInitialNode(Long storyId);
}
