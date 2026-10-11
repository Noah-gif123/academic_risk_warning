package com.example.redchina.controller;

import com.example.redchina.entity.SysUserDesign;
import com.example.redchina.common.ResultVo;
import com.example.redchina.service.SysUserDesignService;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;

/**
 * 用户文创设计控制器（前端交互接口）
 */
@RestController
@RequestMapping("/user/design")
public class SysUserDesignController {

    @Resource
    private SysUserDesignService userDesignService;

    /**
     * 获取当前用户的文创设计列表
     */
    @GetMapping("/list")
    public ResultVo<List<SysUserDesign>> getMyDesigns() {
        // 获取当前登录用户ID（需自定义UserDetails，此处简化为模拟，实际需从认证信息中获取）
        Long userId = getCurrentUserId();
        List<SysUserDesign> designs = userDesignService.getUserDesigns(userId);
        return ResultVo.success(designs);
    }

    /**
     * 上传文创设计
     */
    @PostMapping("/upload")
    public ResultVo<SysUserDesign> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("designName") String designName,
            @RequestParam("tags") String tags,
            @RequestParam(value = "languageType", defaultValue = "zh_CN") String languageType) {
        Long userId = getCurrentUserId();
        return userDesignService.uploadDesign(file, userId, designName, tags, languageType);
    }

    /**
     * 根据标签推荐设计
     */
    @GetMapping("/recommend")
    public ResultVo<List<SysUserDesign>> recommend(@RequestParam("tag") String tag) {
        List<SysUserDesign> designs = userDesignService.recommendByTag(tag);
        return ResultVo.success(designs);
    }

    /**
     * 删除文创设计
     */
    @DeleteMapping("/{id}")
    public ResultVo<?> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return userDesignService.deleteDesign(id, userId);
    }

    /**
     * 辅助方法：获取当前登录用户ID（实际项目需扩展Spring Security的UserDetails）
     */
    private Long getCurrentUserId() {
        // 简化实现：从认证信息中获取用户名，再查询用户ID（实际需优化）
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        // 此处需调用UserService查询用户ID，简化为返回测试用户ID=2
        return "admin".equals(username) ? 1L : 2L;
    }
}
