// src/main/java/com/example/redchina/controller/StoryNodeController.java
package com.example.redchina.controller;

import com.example.redchina.common.ResultVo;
import com.example.redchina.entity.*;
import com.example.redchina.service.*;

import jakarta.annotation.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/story/node")
public class StoryNodeController {

    @Resource
    private SysStoryNodeService storyNodeService;
    @Resource
    private SysStoryOptionService storyOptionService;
    @Resource
    private SysStoryKnowledgeService storyKnowledgeService;
    @Resource
    private SysUnlockStoryService unlockStoryService;

    // 获取剧情初始节点
    @GetMapping("/initial/{storyId}")
    public ResultVo<?> getInitialNode(@PathVariable Long storyId) {
        SysStoryNode initialNode = storyNodeService.getInitialNode(storyId);
        if (initialNode == null) {
            return ResultVo.error("剧情节点不存在");
        }
        // 记录用户解锁节点
        Long userId = getCurrentUserId();
        saveUnlockRecord(userId, storyId, initialNode.getId(), false);
        return ResultVo.success(initialNode);
    }

    // 获取节点选项
    @GetMapping("/options/{nodeId}")
    public ResultVo<?> getNodeOptions(@PathVariable Long nodeId) {
        List<SysStoryOption> options = storyOptionService.getByNodeId(nodeId);
        return ResultVo.success(options);
    }

    // 选择选项后跳转下一个节点
    @PostMapping("/next")
    public ResultVo<?> nextNode(@RequestParam Long optionId) {
        SysStoryOption option = storyOptionService.getById(optionId);
        if (option == null) {
            return ResultVo.error("选项不存在");
        }
        SysStoryNode nextNode = storyNodeService.getById(option.getNextNodeId());
        if (nextNode == null) {
            return ResultVo.error("节点不存在");
        }
        // 记录解锁状态
        Long userId = getCurrentUserId();
        boolean isComplete = nextNode.getIsEnd() == 1; // 如果是结束节点则标记完成
        saveUnlockRecord(userId, nextNode.getStoryId(), nextNode.getId(), isComplete);
        return ResultVo.success(nextNode);
    }

    // 获取节点知识点
    @GetMapping("/knowledge/{nodeId}")
    public ResultVo<?> getNodeKnowledge(@PathVariable Long nodeId) {
        String language = getCurrentUserLanguage();
        SysStoryKnowledge knowledge = storyKnowledgeService.getByNodeIdAndLanguage(nodeId, language);
        return ResultVo.success(knowledge);
    }

    // 保存用户解锁记录
    private void saveUnlockRecord(Long userId, Long storyId, Long nodeId, boolean isComplete) {
        SysUnlockStory unlock = new SysUnlockStory();
        unlock.setUserId(userId);
        unlock.setStoryId(storyId);
        unlock.setUnlockNodeId(nodeId);
        unlock.setIsComplete(isComplete ? 1 : 0);
        unlock.setUnlockTime(LocalDateTime.now());
        unlockStoryService.saveOrUpdate(unlock);
    }

    // 获取当前用户ID
    private Long getCurrentUserId() {
        return ((SysUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }

    // 获取当前用户语言
    private String getCurrentUserLanguage() {
        return ((SysUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getLanguage();
    }
}