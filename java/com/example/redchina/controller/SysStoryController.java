package com.example.redchina.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.redchina.common.ResultVo;
import com.example.redchina.entity.SysStory;
import com.example.redchina.service.SysStoryService;

import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Controller
@RequestMapping("/admin/story")
public class SysStoryController {

    @Resource
    private SysStoryService storyService;

    // 前台：剧情列表页
    @GetMapping("/list")
    public String storyList(@RequestParam(defaultValue = "1") int pageNum,
                            @RequestParam(defaultValue = "10") int pageSize,
                            @RequestParam(required = false) Long tagId,
                            @RequestParam(required = false) Integer isRecommend,
                            Model model) {
        IPage<SysStory> page = new Page<>(pageNum, pageSize);
        IPage<SysStory> storyPage = storyService.selectStoryPage(page, tagId, isRecommend);
        model.addAttribute("page", storyPage);
        model.addAttribute("tagId", tagId);
        model.addAttribute("isRecommend", isRecommend);
        return "admin/story/list";
    }

    // 前台：剧情详情页
    @GetMapping("/detail/{id}")
    public String storyDetail(@PathVariable Long id, Model model) {
        SysStory story = storyService.getStoryDetail(id);
        if (story == null) {
            return "error/404";
        }
        model.addAttribute("story", story);
        return "story/detail";
    }

    // 前台：热门剧情接口
    @GetMapping("/hot")
    @ResponseBody
    public ResultVo<?> getHotStories(@RequestParam(defaultValue = "5") Integer limit) {
        List<SysStory> hotStories = storyService.getHotStories(limit);
        return ResultVo.success(hotStories);
    }

    // 管理员：剧情管理列表
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/list")
    public String adminStoryList(@RequestParam(defaultValue = "1") int pageNum,
                                 @RequestParam(defaultValue = "10") int pageSize,
                                 Model model) {
        IPage<SysStory> page = new Page<>(pageNum, pageSize);
        IPage<SysStory> storyPage = storyService.selectStoryPage(page, null, null);
        model.addAttribute("page", storyPage);
        return "story/admin/list";
    }

    // 管理员：新增/编辑剧情页面
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/edit/{id}")
    public String editStory(@PathVariable(required = false) Long id, Model model) {
        if (id != null) {
            SysStory story = storyService.getById(id);
            model.addAttribute("story", story);
        }
        return "story/admin/edit";
    }

    // 管理员：保存剧情
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/save")
    @ResponseBody
    public ResultVo<?> saveStory(@RequestBody SysStory story) {
        boolean success = storyService.saveStory(story);
        return success ? ResultVo.success(null) : ResultVo.error("操作失败");
    }

    // 管理员：更新剧情状态
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/updateStatus")
    @ResponseBody
    public ResultVo<?> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        boolean success = storyService.updateStatus(id, status);
        return success ? ResultVo.success(null) : ResultVo.error("操作失败");
    }
}