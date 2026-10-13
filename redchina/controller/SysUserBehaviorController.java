package com.example.redchina.controller;

import com.example.redchina.entity.SysUserBehavior;
import com.example.redchina.common.ResultVo;
import com.example.redchina.service.SysUserBehaviorService;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


import java.util.List;

/**
 * 用户行为控制器（记录行为、统计偏好）
 */
@RestController
@RequestMapping("/user/behavior")
public class SysUserBehaviorController {

    @Resource
    private SysUserBehaviorService userBehaviorService;

    /**
     * 记录用户行为（前端调用，如浏览剧情、完成互动）
     */
    @PostMapping("/record")
    public ResultVo<?> recordBehavior(
            @RequestParam String behaviorType,
            @RequestParam Long targetId,
            @RequestParam String targetType) {
        Long userId = getCurrentUserId();
        userBehaviorService.recordBehavior(userId, behaviorType, targetId, targetType);
        return ResultVo.success();
    }

    /**
     * 查询用户行为记录
     */
    @GetMapping("/list")
    public ResultVo<List<SysUserBehavior>> getBehaviorList(@RequestParam String targetType) {
        Long userId = getCurrentUserId();
        List<SysUserBehavior> behaviors = userBehaviorService.getUserBehaviorByTargetType(userId, targetType);
        return ResultVo.success(behaviors);
    }

    /**
     * 获取用户最常互动的目标ID（用于推荐）
     */
    @GetMapping("/top-target")
    public ResultVo<Long> getTopTargetId(@RequestParam String targetType) {
        Long userId = getCurrentUserId();
        Long topTargetId = userBehaviorService.getUserTopTargetId(userId, targetType);
        return ResultVo.success(topTargetId);
    }

    /**
     * 辅助方法：获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        // 简化实现：实际需从UserService查询用户ID
        return "admin".equals(username) ? 1L : 2L;
    }
}