package com.example.redchina.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;


import com.example.redchina.entity.SysUser;
import com.example.redchina.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;


import java.time.LocalDateTime;

@Controller
public class LoginController {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private PasswordEncoder passwordEncoder;

    // 跳转登录页面
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // 对应templates/login.html
    }

    // 跳转注册页面
    @GetMapping("/register")
    public String registerPage() {
        return "register"; // 对应templates/register.html
    }

    // 处理注册请求
    @PostMapping("/doRegister")
    public String register(SysUser sysUser, Model model) {
        // 1. 校验用户名是否重复
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, sysUser.getUsername());
        if (sysUserService.count(queryWrapper) > 0) {
            model.addAttribute("error", "用户名已存在");
            return "register";
        }

        // 2. 加密密码
        sysUser.setPassword(passwordEncoder.encode(sysUser.getPassword()));

        // 3. 初始化字段
        sysUser.setStatus(1); // 正常状态
        sysUser.setRole(0); // 默认普通用户
        sysUser.setUserTag(""); // 默认空标签
        sysUser.setCreateTime(LocalDateTime.now());
        sysUser.setUpdateTime(LocalDateTime.now());

        // 4. 保存用户
        sysUserService.save(sysUser);

        // 5. 注册成功跳转登录页
        return "redirect:/login?registerSuccess=true";
    }
}
