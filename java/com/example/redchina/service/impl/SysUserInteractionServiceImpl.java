package com.example.redchina.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.redchina.entity.SysUserInteraction;
import com.example.redchina.exception.BusinessException;
import com.example.redchina.mapper.SysUserInteractionMapper;
import com.example.redchina.service.SysUserInteractionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 用户-互动关卡结果Service实现
 */
@Service
public class SysUserInteractionServiceImpl extends ServiceImpl<SysUserInteractionMapper, SysUserInteraction> implements SysUserInteractionService {

    @Resource
    private SysUserInteractionMapper userInteractionMapper;

    @Override
    public boolean saveOrUpdateUserInteraction(SysUserInteraction userInteraction) {
        if (userInteraction == null || userInteraction.getUserId() == null || userInteraction.getInteractionId() == null) {
            throw new BusinessException("用户ID和互动关卡ID不能为空");
        }
        // 查询是否已参与该关卡
        SysUserInteraction exist = userInteractionMapper.selectByUserAndInteraction(
                userInteraction.getUserId(), userInteraction.getInteractionId());
        if (exist != null) {
            // 已参与，更新结果
            userInteraction.setId(exist.getId());
            return updateById(userInteraction);
        } else {
            // 未参与，新增记录
            return save(userInteraction);
        }
    }

    @Override
    public List<SysUserInteraction> getUserInteractionByUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        return userInteractionMapper.selectByUserId(userId);
    }

    @Override
    public SysUserInteraction getUserInteractionByUserAndInteraction(Long userId, Long interactionId) {
        if (userId == null || interactionId == null) {
            throw new BusinessException("用户ID和互动关卡ID不能为空");
        }
        return userInteractionMapper.selectByUserAndInteraction(userId, interactionId);
    }

    @Override
    public IPage<SysUserInteraction> getUserInteractionPage(IPage<SysUserInteraction> page, Long userId, Long interactionId) {
        // 1. 创建 QueryWrapper 实例（指定实体类）
        QueryWrapper<SysUserInteraction> queryWrapper = new QueryWrapper<>();

        // 2. 设置查询条件（非空判断后添加）
        if (userId != null) {
            queryWrapper.eq("user_id", userId); // 字段名对应数据库列名（或实体属性名）
        }
        if (interactionId != null) {
            queryWrapper.eq("interaction_id", interactionId);
        }

        // 3. 设置排序
        queryWrapper.orderByDesc("create_time");

        // 4. 调用 mapper 的 selectPage 方法（分页查询需用 selectPage 而非 selectList）
        return userInteractionMapper.selectPage(page, queryWrapper);
    }

    @Override
    public List<SysUserInteraction> getUserSuccessInteractions(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        return userInteractionMapper.selectByUserIdAndSuccess(userId, 1);
    }

    @Override
    public Double calculateInteractionPassRate(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        // 查询用户参与的总关卡数
        List<SysUserInteraction> totalList = userInteractionMapper.selectByUserId(userId);
        if (totalList.isEmpty()) {
            return 0.0;
        }
        // 查询成功通过的关卡数
        List<SysUserInteraction> successList = userInteractionMapper.selectByUserIdAndSuccess(userId, 1);
        // 计算通过率（保留2位小数）
        return Math.round(((double) successList.size() / totalList.size()) * 100) / 100.0;
    }
}