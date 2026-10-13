package com.example.redchina.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.redchina.entity.SysTag;
import com.example.redchina.entity.SysUser;
import com.example.redchina.entity.SysUserTag;
import com.example.redchina.mapper.SysTagMapper;
import com.example.redchina.mapper.SysUserMapper;
import com.example.redchina.mapper.SysUserTagMapper;
import com.example.redchina.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Resource
    private SysUserTagMapper userTagMapper;
    @Resource
    private SysTagMapper tagMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private SysUserMapper userMapper;

    @Override
    @Transactional
    public boolean register(SysUser user) {
        // 1. 校验用户名/邮箱唯一性
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, user.getUsername())
                .or()
                .eq(SysUser::getEmail, user.getEmail());
        if (count(queryWrapper) > 0) {
            return false; // 用户名或邮箱已存在
        }

        // 2. 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 3. 设置默认值
        user.setRole(1);
        user.setLanguage("zh_CN");
        user.setStatus(1);
        // 4. 保存用户
        boolean saveSuccess = save(user);
        if (!saveSuccess) return false;

        // 5. 给新用户分配默认标签（中华文化入门）
        LambdaQueryWrapper<SysTag> tagQuery = new LambdaQueryWrapper<>();
        tagQuery.eq(SysTag::getTagName, "中华文化入门");
        SysTag defaultTag = tagMapper.selectOne(tagQuery);
        if (defaultTag != null) {
            SysUserTag userTag = new SysUserTag();
            userTag.setUserId(user.getId());
            userTag.setTagId(defaultTag.getId());
            userTagMapper.insert(userTag);
        }

        return true;
    }

    @Override
    public SysUser getByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }

    @Override
    public List<String> getTagNamesByUserId(Long userId) {
        return baseMapper.selectTagNamesByUserId(userId);
    }

    @Override
    public IPage<SysUser> selectUserPage(IPage<SysUser> page, String keyword) {
        return baseMapper.selectUserPage(page, keyword);
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        return updateById(user);
    }

    @Override
    @Transactional
    public boolean assignTags(Long userId, List<Long> tagIds) {
        // 1. 删除用户原有标签
        userTagMapper.deleteByUserId(userId);
        // 2. 批量插入新标签
        if (tagIds == null || tagIds.isEmpty()) {
            return true;
        }
        List<SysUserTag> userTagList = new ArrayList<>();
        for (Long tagId : tagIds) {
            SysUserTag userTag = new SysUserTag();
            userTag.setUserId(userId);
            userTag.setTagId(tagId);
            userTagList.add(userTag);
        }
        return userTagMapper.insertBatch(userTagList) > 0;
    }

    @Override
    @Transactional
    public boolean updateProfile(SysUser user) {
        // 只允许更新昵称、邮箱、手机号、语言、头像
        SysUser dbUser = getById(user.getId());
        if (dbUser == null) return false;
        dbUser.setNickname(user.getNickname());
        dbUser.setEmail(user.getEmail());
        dbUser.setPhone(user.getPhone());
        dbUser.setLanguage(user.getLanguage());
        if (user.getAvatar() != null) {
            dbUser.setAvatar(user.getAvatar());
        }
        return updateById(dbUser);
    }

    // 补充实现：
    @Override
    public boolean existsUsername(String username) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, username);
        return count(queryWrapper) > 0;
    }

    @Override
    public boolean existsEmail(String email) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getEmail, email);
        return count(queryWrapper) > 0;
    }

    @Override
    public List<Long> getTagIdsByUserId(Long userId) {
        return userTagMapper.selectTagIdsByUserId(userId);
    }

    @Override
    @Transactional
    public boolean saveUserTags(List<SysUserTag> userTagList) {
        if (userTagList == null || userTagList.isEmpty()) {
            return true;
        }
        return userTagMapper.insertBatch(userTagList) > 0;
    }

    @Override
    @Transactional
    public boolean removeUserTags(Long userId) {
        return userTagMapper.deleteByUserId(userId) > 0;
    }

    @Override
    public SysUser getUserByUsername(String username) {
        return userMapper.selectOne(
                Wrappers.lambdaQuery(SysUser.class) // 修正：通过工具类创建
                        .eq(SysUser::getUsername, username)
        );
    }
}