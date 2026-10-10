package com.example.redchina.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.redchina.entity.SysStoryKnowledge;


import java.util.List;

public interface SysStoryKnowledgeService extends IService<SysStoryKnowledge> {
    // 根据节点ID和语言类型查询知识点
    SysStoryKnowledge getByNodeIdAndLanguage(Long storyNodeId, String languageType);

    // 根据剧情ID查询所有知识点
    List<SysStoryKnowledge> getByStoryId(Long storyId);

    // 管理员批量保存知识点（含多语言）
    boolean saveBatchKnowledge(List<SysStoryKnowledge> knowledgeList);

    // 根据节点ID删除知识点
    boolean removeByNodeId(Long storyNodeId);
}
