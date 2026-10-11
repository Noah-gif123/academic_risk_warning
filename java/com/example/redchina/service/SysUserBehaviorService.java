package com.example.redchina.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.redchina.entity.SysUserBehavior;
import java.util.List;

public interface SysUserBehaviorService extends IService<SysUserBehavior> {
    // 记录用户行为
    void recordBehavior(Long userId, String behaviorType, Long targetId, String targetType);

    // 根据用户ID和目标类型查询行为记录
    List<SysUserBehavior> getUserBehaviorByTargetType(Long userId, String targetType);

    // 获取用户最常互动的目标ID（用于推荐）
    Long getUserTopTargetId(Long userId, String targetType);
}