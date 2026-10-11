
package com.example.redchina.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.example.redchina.entity.SysStory;
import com.example.redchina.entity.SysUser;
import com.example.redchina.service.SysStoryService;
import com.example.redchina.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


import java.util.List;

@Controller
@RequestMapping("/recommend")
public class RecommendController {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private SysStoryService sysStoryService;

    // 基础推荐：根据用户标签匹配剧情标签
    @GetMapping("/story")
    public String recommendStory(Model model) {
        // 1. 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // 2. 查询用户标签
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username);
        SysUser user = sysUserService.getOne(userWrapper);
        String userTag = user.getUserTag();

        // 3. 匹配剧情标签（基础版：包含任意标签即推荐）
        LambdaQueryWrapper<SysStory> storyWrapper = new LambdaQueryWrapper<SysStory>();
        if (!userTag.isEmpty()) {
            String[] tags = userTag.split(",");
            storyWrapper.in(SysStory::getTag, tags);
        }
        List<SysStory> recommendStories = sysStoryService.list(storyWrapper);

        // 4. 传递到页面
        model.addAttribute("recommendStories", recommendStories);
        return "recommend/story"; // 对应templates/recommend/story.html
    }
}
