package com.example.redchina.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.redchina.entity.SysInteraction;
import com.example.redchina.entity.SysStoryNode;
import com.example.redchina.mapper.SysInteractionMapper;
import com.example.redchina.mapper.SysStoryNodeMapper;
import com.example.redchina.service.SysInteractionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 互动关卡服务实现类
 */
@Service
public class SysInteractionServiceImpl extends ServiceImpl<SysInteractionMapper, SysInteraction> implements SysInteractionService {

    @Resource
    private SysInteractionMapper interactionMapper;

    @Resource
    private SysStoryNodeMapper storyNodeMapper;

    @Override
    public SysInteraction getByStoryNodeId(Long storyNodeId) {
        if (storyNodeId == null) {
            return null;
        }
        return interactionMapper.selectByStoryNodeId(storyNodeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addInteraction(SysInteraction interaction) {
        // 参数校验
        if (interaction == null
                || interaction.getStoryNodeId() == null
                || interaction.getInteractionType() == null
                || interaction.getContent() == null
                || interaction.getSuccessNodeId() == null
                || interaction.getFailNodeId() == null) {
            throw new IllegalArgumentException("互动参数不能为空");
        }

        // 校验关联节点是否存在
        SysStoryNode storyNode = storyNodeMapper.selectById(interaction.getStoryNodeId());
        SysStoryNode successNode = storyNodeMapper.selectById(interaction.getSuccessNodeId());
        SysStoryNode failNode = storyNodeMapper.selectById(interaction.getFailNodeId());
        if (storyNode == null || successNode == null || failNode == null) {
            throw new RuntimeException("关联的剧情节点不存在");
        }

        // 校验互动类型（仅支持QUESTION/GAME）
        if (!"QUESTION".equals(interaction.getInteractionType())
                && !"GAME".equals(interaction.getInteractionType())) {
            throw new RuntimeException("互动类型仅支持QUESTION或GAME");
        }

        return save(interaction);
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateInteraction(SysInteraction interaction) {
        // 校验ID是否存在（使用baseMapper直接查询，最可靠）
        if (interaction.getId() == null) {
            throw new RuntimeException("互动关卡ID不能为空");
        }
        // 直接通过mapper查询是否存在
        SysInteraction existing = baseMapper.selectById(interaction.getId());
        if (existing == null) {
            throw new RuntimeException("互动关卡不存在");
        }

        // 复用新增的参数校验逻辑
        addInteraction(interaction);
        return updateById(interaction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAddInteraction(List<SysInteraction> interactionList) {
        if (interactionList == null || interactionList.isEmpty()) {
            return 0;
        }
        // 批量插入前校验每个互动的参数
        for (SysInteraction interaction : interactionList) {
            addInteraction(interaction);
        }
        return interactionMapper.batchInsert(interactionList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByStoryId(Long storyId) {
        if (storyId == null) {
            return 0;
        }
        // 先查询剧情下所有节点ID，再删除关联互动
        LambdaQueryWrapper<SysStoryNode> nodeQuery = new LambdaQueryWrapper<>();
        nodeQuery.eq(SysStoryNode::getStoryId, storyId);
        List<SysStoryNode> nodeList = storyNodeMapper.selectList(nodeQuery);

        if (nodeList.isEmpty()) {
            return 0;
        }

        LambdaQueryWrapper<SysInteraction> interactionQuery = new LambdaQueryWrapper<>();
        interactionQuery.in(SysInteraction::getStoryNodeId,
                nodeList.stream().map(SysStoryNode::getId).toList());

        return interactionMapper.delete(interactionQuery);
    }
}
