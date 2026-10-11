package com.example.redchina.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.example.redchina.entity.SysUser;
import com.example.redchina.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;


@Controller
@RequestMapping("/admin/user")
@PreAuthorize("hasRole('ADMIN')") // 仅管理员可访问
public class AdminUserController {

    @Resource
    private SysUserService sysUserService;

    // 用户列表（分页）
    @GetMapping("/list")
    public String userList(Model model,
                           @RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<SysUser> page = new Page<>(pageNum, pageSize);
        sysUserService.page(page, new LambdaQueryWrapper<SysUser>());
        model.addAttribute("page", page);
        return "admin/user/list"; // 对应templates/admin/user/list.html
    }

    // 跳转新增/编辑页面
    @GetMapping("/form/{id}")
    public String userForm(@PathVariable(required = false) Long id, Model model) {
        if (id != null) {
            model.addAttribute("user", sysUserService.getById(id));
        }
        return "admin/user/form"; // 对应templates/admin/user/form.html
    }

    // 保存/更新用户
    @PostMapping("/save")
    public String saveUser(SysUser sysUser) {
        // 编辑时如果密码为空，不修改密码
        if (sysUser.getId() != null && sysUser.getPassword().isEmpty()) {
            SysUser oldUser = sysUserService.getById(sysUser.getId());
            sysUser.setPassword(oldUser.getPassword());
        } else {
            // 新增/修改密码时加密
            sysUser.setPassword(passwordEncoder.encode(sysUser.getPassword()));
        }
        sysUser.setUpdateTime(LocalDateTime.now());
        sysUserService.saveOrUpdate(sysUser);
        return "redirect:/admin/user/list";
    }

    // 删除用户
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        sysUserService.removeById(id);
        return "redirect:/admin/user/list";
    }

    @Resource
    private PasswordEncoder passwordEncoder;
}
