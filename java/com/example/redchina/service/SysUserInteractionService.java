package com.example.redchina.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.redchina.entity.SysUserInteraction;
import java.util.List;

/**
 * 用户-互动关卡结果Service
 */
public interface SysUserInteractionService extends IService<SysUserInteraction> {

    // 新增用户互动结果（若已参与则更新）
    boolean saveOrUpdateUserInteraction(SysUserInteraction userInteraction);

    // 按用户ID查询互动记录
    List<SysUserInteraction> getUserInteractionByUserId(Long userId);

    // 按用户ID+互动关卡ID查询
    SysUserInteraction getUserInteractionByUserAndInteraction(Long userId, Long interactionId);

    // 分页查询用户互动记录
    IPage<SysUserInteraction> getUserInteractionPage(IPage<SysUserInteraction> page, Long userId, Long interactionId);

    // 查询用户成功通过的互动关卡
    List<SysUserInteraction> getUserSuccessInteractions(Long userId);

    // 统计用户互动关卡通过率
    Double calculateInteractionPassRate(Long userId);
}
