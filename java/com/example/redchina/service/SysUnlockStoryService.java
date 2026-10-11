package com.example.redchina.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.redchina.entity.SysUnlockStory;

import java.util.List;

public interface SysUnlockStoryService extends IService<SysUnlockStory> {
    // 根据用户ID和剧情ID查询解锁记录
    SysUnlockStory getByUserAndStory(Long userId, Long storyId);

    // 根据用户ID查询已解锁的剧情列表
    List<SysUnlockStory> getByUserId(Long userId);

    // 根据用户ID和完成状态查询解锁剧情
    List<SysUnlockStory> getByUserIdAndCompleteStatus(Long userId, Integer isComplete);

    // 分页查询用户解锁记录（管理员统计用）
    IPage<SysUnlockStory> selectUnlockStoryPage(IPage<SysUnlockStory> page, Long userId, Long storyId);

    // 根据剧情ID查询解锁该剧情的用户数量
    int getUserCountByStoryId(Long storyId);

    // 更新剧情完成状态
    boolean updateCompleteStatus(Long userId, Long storyId, Long unlockNodeId, Integer isComplete);

    // 解锁新剧情节点
    boolean unlockStoryNode(Long userId, Long storyId, Long unlockNodeId);
}