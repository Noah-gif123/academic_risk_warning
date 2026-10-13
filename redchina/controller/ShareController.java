// src/main/java/com/example/redchina/controller/ShareController.java
package com.example.redchina.controller;

import com.example.redchina.common.ResultVo;
import com.example.redchina.entity.SysUser;
import com.example.redchina.service.ShareService;

import jakarta.annotation.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/share")
public class ShareController {

    @Resource
    private ShareService shareService;

    @GetMapping("/story/{storyId}")
    public ResultVo<?> shareStory(@PathVariable Long storyId) {
        try {
            Long userId = ((SysUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
            String language = ((SysUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getLanguage();
            String imagePath = shareService.generateStoryShareImage(userId, storyId, language);
            return ResultVo.success(imagePath);
        } catch (Exception e) {
            return ResultVo.error("生成分享图片失败：" + e.getMessage());
        }
    }
}
