package com.example.redchina.controller;

import com.example.redchina.common.ResultVo;
import com.example.redchina.entity.SysUser;
import com.example.redchina.entity.SysUserTag;
import com.example.redchina.exception.BusinessException;
import com.example.redchina.service.SysUserService;
import com.example.redchina.service.SysUserTagService;

import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户Controller（登录、注册、退出、用户管理）
 */
@RestController
@RequestMapping("/user")
public class SysUserController {

    @Resource
    private SysUserService userService;

    @Resource
    private SysUserTagService userTagService;

    @Resource
    private PasswordEncoder passwordEncoder;

    // 注册
    @PostMapping("/register")
    public ResultVo<Boolean> register(@RequestBody SysUser user) {
        // 校验用户名是否已存在
        if (userService.getUserByUsername(user.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        // 密码加密（BCrypt）
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 默认角色为普通用户，状态为正常
        user.setRole(1);
        user.setStatus(1);
        // 默认语言为中文
        if (user.getLanguage() == null) {
            user.setLanguage("zh_CN");
        }
        return ResultVo.success(userService.save(user));
    }

    // 登录（实际由Spring Security处理，此处返回用户信息）
    @GetMapping("/info")
    public ResultVo<SysUser> getUserInfo(@RequestParam String username) {
        SysUser user = userService.getUserByUsername(username);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 隐藏密码
        user.setPassword(null);
        return ResultVo.success(user);
    }

    // 绑定用户标签（用于个性化推荐）
    @PostMapping("/bindTag")
    public ResultVo<Boolean> bindUserTag(@RequestParam Long userId, @RequestParam List<Long> tagIds) {
        if (userId == null || tagIds.isEmpty()) {
            throw new BusinessException("用户ID和标签ID不能为空");
        }
        // 先删除原有标签绑定
        userTagService.removeByUserId(userId);
        // 批量新增标签绑定
        List<SysUserTag> userTagList = tagIds.stream().map(tagId -> {
            SysUserTag userTag = new SysUserTag();
            userTag.setUserId(userId);
            userTag.setTagId(tagId);
            return userTag;
        }).toList();
        return ResultVo.success(userTagService.saveBatch(userTagList));
    }

    // 管理员修改用户状态（启用/禁用）
    @PutMapping("/status/{id}/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResultVo<Boolean> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        SysUser user = userService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        return ResultVo.success(userService.updateById(user));
    }
}