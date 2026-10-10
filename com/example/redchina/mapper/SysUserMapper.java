package com.example.redchina.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.redchina.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserMapper extends BaseMapper<SysUser> {

    // 根据用户名查询用户（登录用）
    SysUser selectByUsername(String username);

    // 根据用户ID查询标签名称列表
    List<String> selectTagNamesByUserId(Long userId);

    // 分页查询用户（支持关键词搜索）
    IPage<SysUser> selectUserPage(IPage<SysUser> page, @Param("keyword") String keyword);

    // 分页查询用户（带条件构造器，可选）
    IPage<SysUser> selectUserPage(Page<SysUser> page, @Param(Constants.WRAPPER) Wrapper<SysUser> queryWrapper);
}