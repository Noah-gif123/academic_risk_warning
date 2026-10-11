package com.example.redchina.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.redchina.entity.SysStoryOption;

import java.util.List;

public interface SysStoryOptionService extends IService<SysStoryOption> {
    // 根据节点ID查询选项列表
    List<SysStoryOption> getByNodeId(Long nodeId);

    // 根据节点ID和下一个节点ID查询选项
    SysStoryOption getByNodeIdAndNextNodeId(Long nodeId, Long nextNodeId);

    // 根据剧情ID查询所有选项
    List<SysStoryOption> getByStoryId(Long storyId);

    // 批量删除节点对应的选项
    boolean removeBatchByNodeIds(List<Long> nodeIds);

    // 批量删除剧情对应的选项
    boolean removeBatchByStoryIds(List<Long> storyIds);
}
