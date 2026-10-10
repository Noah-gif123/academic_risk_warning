package com.example.redchina.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.redchina.entity.SysUser;
import com.example.redchina.entity.SysUserTag;

import java.util.List;

public interface SysUserService extends IService<SysUser> {
    // 用户注册
    boolean register(SysUser user);

    // 根据用户名查询用户（登录认证用）
    SysUser getByUsername(String username);

    // 根据用户ID查询关联的标签名称
    List<String> getTagNamesByUserId(Long userId);

    // 管理员分页查询用户（支持关键词搜索）
    IPage<SysUser> selectUserPage(IPage<SysUser> page, String keyword);

    // 管理员修改用户状态（禁用/启用）
    boolean updateStatus(Long id, Integer status);

    // 给用户分配标签（管理员功能）
    boolean assignTags(Long userId, List<Long> tagIds);

    // 更新用户个人信息（含语言偏好）
    boolean updateProfile(SysUser user);

    // 补充：校验    // 校验用户名是否存在
    boolean existsUsername(String username);

    // 校验邮箱是否存在
    boolean existsEmail(String email);

    // 根据用户ID查询关联的标签ID
    List<Long> getTagIdsByUserId(Long userId);

    // 批量保存用户标签关联
    boolean saveUserTags(List<SysUserTag> userTagList);

    // 删除用户所有标签关联
    boolean removeUserTags(Long userId);

    SysUser getUserByUsername(String username);


}