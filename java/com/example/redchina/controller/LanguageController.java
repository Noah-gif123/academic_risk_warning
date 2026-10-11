// src/main/java/com/example/redchina/controller/LanguageController.java
package com.example.redchina.controller;

import com.example.redchina.common.ResultVo;
import com.example.redchina.entity.SysUser;
import com.example.redchina.service.SysUserService;

import com.example.redchina.util.MessageUtil;

import jakarta.annotation.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



import java.util.List;

@RestController
@RequestMapping("/language")
public class LanguageController {

    @Resource
    private SysUserService userService;
    @Resource
    private MessageUtil messageUtil;

    // 切换用户默认语言
    @PostMapping("/switch")
    public ResultVo<?> switchLanguage(@RequestParam String language) {
        // 校验语言类型
        if (!List.of("zh_CN", "en_US", "ja_JP", "ko_KR").contains(language)) {
            return ResultVo.error("不支持的语言类型");
        }
        // 更新用户语言设置
        SysUser currentUser = (SysUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SysUser user = new SysUser();
        user.setId(currentUser.getId());
        user.setLanguage(language);
        boolean success = userService.updateById(user);
        return success ? ResultVo.success(null, messageUtil.getMessage("common.success"))
                : ResultVo.error(messageUtil.getMessage("common.error"));
    }
}
