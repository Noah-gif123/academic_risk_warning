package com.example.redchina.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.redchina.entity.SysUnlockStory;
import com.example.redchina.mapper.SysUnlockStoryMapper;
import com.example.redchina.service.SysUnlockStoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysUnlockStoryServiceImpl extends ServiceImpl<SysUnlockStoryMapper, SysUnlockStory> implements SysUnlockStoryService {

    @Resource
    private SysUnlockStoryMapper unlockStoryMapper;

    @Override
    public SysUnlockStory getByUserAndStory(Long userId, Long storyId) {
        return unlockStoryMapper.selectByUserAndStory(userId, storyId);
    }

    @Override
    public List<SysUnlockStory> getByUserId(Long userId) {
        return unlockStoryMapper.selectByUserId(userId);
    }

    @Override
    public List<SysUnlockStory> getByUserIdAndCompleteStatus(Long userId, Integer isComplete) {
        return unlockStoryMapper.selectByUserIdAndCompleteStatus(userId, isComplete);
    }

    @Override
    public IPage<SysUnlockStory> selectUnlockStoryPage(IPage<SysUnlockStory> page, Long userId, Long storyId) {
        return unlockStoryMapper.selectUnlockStoryPage(page, userId, storyId);
    }

    @Override
    public int getUserCountByStoryId(Long storyId) {
        return unlockStoryMapper.selectUserCountByStoryId(storyId);
    }

    @Override
    @Transactional
    public boolean updateCompleteStatus(Long userId, Long storyId, Long unlockNodeId, Integer isComplete) {
        // 先查询是否存在解锁记录
        SysUnlockStory unlockStory = getByUserAndStory(userId, storyId);
        if (unlockStory == null) {
            return false;
        }
        // 更新完成状态和当前解锁节点
        unlockStory.setIsComplete(isComplete);
        unlockStory.setUnlockNodeId(unlockNodeId);
        unlockStory.setUnlockTime(LocalDateTime.now());
        return updateById(unlockStory);
    }

    @Override
    @Transactional
    public boolean unlockStoryNode(Long userId, Long storyId, Long unlockNodeId) {
        // 检查是否已解锁该剧情
        SysUnlockStory existing = getByUserAndStory(userId, storyId);
        if (existing != null) {
            // 更新解锁节点
            existing.setUnlockNodeId(unlockNodeId);
            existing.setUnlockTime(LocalDateTime.now());
            return updateById(existing);
        } else {
            // 新增解锁记录
            SysUnlockStory newUnlock = new SysUnlockStory();
            newUnlock.setUserId(userId);
            newUnlock.setStoryId(storyId);
            newUnlock.setUnlockNodeId(unlockNodeId);
            newUnlock.setIsComplete(0); // 初始为未完成
            newUnlock.setUnlockTime(LocalDateTime.now());
            return save(newUnlock);
        }
    }
}
