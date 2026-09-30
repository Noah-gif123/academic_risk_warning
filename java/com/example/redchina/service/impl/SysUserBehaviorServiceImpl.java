package com.example.redchina.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.redchina.entity.SysUserBehavior;
import com.example.redchina.mapper.SysUserBehaviorMapper;
import com.example.redchina.service.SysUserBehaviorService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysUserBehaviorServiceImpl extends ServiceImpl<SysUserBehaviorMapper, SysUserBehavior> implements SysUserBehaviorService {

    @Resource
    private SysUserBehaviorMapper userBehaviorMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordBehavior(Long userId, String behaviorType, Long targetId, String targetType) {
        SysUserBehavior behavior = new SysUserBehavior();
        behavior.setUserId(userId);
        behavior.setBehaviorType(behaviorType);
        behavior.setTargetId(targetId);
        behavior.setTargetType(targetType);
        save(behavior);
    }

    @Override
    public List<SysUserBehavior> getUserBehaviorByTargetType(Long userId, String targetType) {
        return userBehaviorMapper.selectByUserIdAndTargetType(userId, targetType);
    }

    @Override
    public Long getUserTopTargetId(Long userId, String targetType) {
        String targetIdStr = userBehaviorMapper.UserTargetFrequency(userId, targetType).toString();
        return targetIdStr != null ? Long.parseLong(targetIdStr) : null;
    }
}
