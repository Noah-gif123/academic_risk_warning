package com.example.redchina.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.redchina.entity.SysUserTag;
import com.example.redchina.mapper.SysUserTagMapper;
import com.example.redchina.service.SysUserTagService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;

/**
 * 用户标签关联服务实现类
 */
@Service
public class SysUserTagServiceImpl extends ServiceImpl<SysUserTagMapper, SysUserTag> implements SysUserTagService {

    @Resource
    private SysUserTagMapper userTagMapper;

    @Override
    public List<Long> getUserTagIds(Long userId) {
        // 查询用户关联的标签ID，无结果时返回空列表（避免NullPointerException）
        List<Long> tagIds = userTagMapper.selectTagIdsByUserId(userId);
        return tagIds != null ? tagIds : new ArrayList<>();
    }

    @Override
    public List<String> getUserTagNames(Long userId) {
        // 查询用户关联的标签名称
        return userTagMapper.selectTagNamesByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignTags(Long userId, List<Long> tagIds) {
        // 1. 先删除用户原有标签关联
        userTagMapper.deleteByUserId(userId);

        // 2. 若新标签列表为空，直接返回成功
        if (tagIds == null || tagIds.isEmpty()) {
            return true;
        }

        // 3. 批量添加新的标签关联
        List<SysUserTag> userTagList = new ArrayList<>();
        for (Long tagId : tagIds) {
            SysUserTag userTag = new SysUserTag();
            userTag.setUserId(userId);
            userTag.setTagId(tagId);
            userTagList.add(userTag);
        }

        // 4. 执行批量插入
        return batchAdd(userTagList) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAdd(List<SysUserTag> userTagList) {
        if (userTagList == null || userTagList.isEmpty()) {
            return 0;
        }
        return userTagMapper.insertBatch(userTagList);
    }

    @Override
    public List<Long> getUserIdByTagId(Long tagId) {
        return userTagMapper.selectUserIdsByTagId(tagId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByUserId(Long userId) {
        return userTagMapper.deleteByUserId(userId) > 0;
    }
}
