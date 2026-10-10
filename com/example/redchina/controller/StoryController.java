package com.example.redchina.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.redchina.entity.SysStory;
import com.example.redchina.service.SysStoryService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;



@Controller
@RequestMapping("/story")
public class StoryController {

    @Resource
    private SysStoryService sysStoryService;

    // 剧情列表（普通用户/管理员均可访问）
    @GetMapping("/list")
    public String storyList(Model model,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<SysStory> page = new Page<>(pageNum, pageSize);
        sysStoryService.page(page);
        model.addAttribute("page", page);
        return "story/list"; // 对应templates/story/list.html
    }

    // 剧情详情（解锁剧情）
    @GetMapping("/detail/{id}")
    public String storyDetail(@PathVariable Long id, Model model) {
        SysStory story = sysStoryService.getById(id);
        model.addAttribute("story", story);
        // 基础版：模拟解锁剧情（实际需关联用户解锁记录，此处简化）
        return "story/detail"; // 对应templates/story/detail.html
    }

    // 管理员-新增/编辑剧情
    @GetMapping("/admin/form/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String storyForm(@PathVariable(required = false) Long id, Model model) {
        if (id != null) {
            model.addAttribute("story", sysStoryService.getById(id));
        }
        return "story/form"; // 对应templates/story/form.html
    }

    // 管理员-保存剧情
    @PostMapping("/admin/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveStory(SysStory story) {
        sysStoryService.saveOrUpdate(story);
        return "redirect:/story/list";
    }

    // 管理员-删除剧情
    @GetMapping("/admin/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteStory(@PathVariable Long id) {
        sysStoryService.removeById(id);
        return "redirect:/story/list";
    }
}
