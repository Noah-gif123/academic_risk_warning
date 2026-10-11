package com.example.redchina.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.redchina.common.ResultVo;
import com.example.redchina.entity.SysUserInteraction;
import com.example.redchina.service.SysUserInteractionService;

import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户-互动关卡结果Controller（用户端+管理员端）
 */
@RestController
@RequestMapping("/user/interaction")
public class SysUserInteractionController {

    @Resource
    private SysUserInteractionService userInteractionService;

    // 新增/更新用户互动结果（用户端）
    @PostMapping("/save")
    public ResultVo<Boolean> saveOrUpdate(@RequestBody SysUserInteraction userInteraction) {
        return ResultVo.success(userInteractionService.saveOrUpdateUserInteraction(userInteraction));
    }

    // 按用户ID查询互动记录（用户端-个人中心）
    @GetMapping("/listByUser")
    public ResultVo<List<SysUserInteraction>> listByUserId(@RequestParam Long userId) {
        return ResultVo.success(userInteractionService.getUserInteractionByUserId(userId));
    }

    // 按用户ID+互动关卡ID查询（用户端-判断是否已参与）
    @GetMapping("/getByUserAndInteraction")
    public ResultVo<SysUserInteraction> getByUserAndInteraction(
            @RequestParam Long userId,
            @RequestParam Long interactionId) {
        return ResultVo.success(userInteractionService.getUserInteractionByUserAndInteraction(userId, interactionId));
    }

    // 查询用户成功通过的互动关卡（用户端-个人成就）
    @GetMapping("/successList")
    public ResultVo<List<SysUserInteraction>> getSuccessList(@RequestParam Long userId) {
        return ResultVo.success(userInteractionService.getUserSuccessInteractions(userId));
    }

    // 统计用户互动关卡通过率（用户端-个人中心）
    @GetMapping("/passRate")
    public ResultVo<Double> getPassRate(@RequestParam Long userId) {
        return ResultVo.success(userInteractionService.calculateInteractionPassRate(userId));
    }

    // 分页查询用户互动记录（管理员端）
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public ResultVo<IPage<SysUserInteraction>> getPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long interactionId) {
        IPage<SysUserInteraction> page = new Page<>(pageNum, pageSize);
        return ResultVo.success(userInteractionService.getUserInteractionPage(page, userId, interactionId));
    }

    // 删除用户互动记录（管理员端）
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResultVo<Boolean> delete(@PathVariable Long id) {
        return ResultVo.success(userInteractionService.removeById(id));
    }
}
