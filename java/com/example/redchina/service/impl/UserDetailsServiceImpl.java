
package com.example.redchina.service.impl;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.example.redchina.entity.SysUser;
import com.example.redchina.mapper.SysUserMapper;
import jakarta.annotation.Resource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 根据用户名查询用户
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username);
        SysUser sysUser = sysUserMapper.selectOne(queryWrapper);

        // 用户不存在
        if (sysUser == null) {
            throw new UsernameNotFoundException("用户名不存在");
        }

        // 用户禁用
        if (sysUser.getStatus() == 0) {
            throw new UsernameNotFoundException("账号已禁用");
        }

        // 封装角色（ROLE_前缀是Spring Security规范）
        String role = sysUser.getRole() == 1 ? "ROLE_ADMIN" : "ROLE_USER";

        // 返回UserDetails（Spring Security标准对象）
        return new User(
                sysUser.getUsername(),
                sysUser.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }
}