package com.example.redchina.controller;


import com.example.redchina.common.ResultVo;
import com.example.redchina.service.SysCulturalTermService;

import com.example.redchina.entity.SysCulturalTerm;
import com.example.redchina.entity.SysUser;
import jakarta.annotation.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


import java.util.List;

@Controller
@RequestMapping("/term")
public class SysCulturalTermController {

    @Resource
    private SysCulturalTermService termService;

    // 术语词典页面
    @GetMapping("/dictionary")
    public String termDictionary(Model model) {
        // 获取当前用户语言
        SysUser currentUser = (SysUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        model.addAttribute("language", currentUser.getLanguage());
        return "term/dictionary";
    }

    // 模糊查询术语
    @GetMapping("/search")
    @ResponseBody
    public ResultVo<List<SysCulturalTerm>> searchTerm(@RequestParam String keyword) {
        List<SysCulturalTerm> terms = termService.selectByKeyword(keyword);
        return ResultVo.success(terms);
    }
}
