package com.example.redchina.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.redchina.common.ResultVo;
import com.example.redchina.entity.SysInteraction;
import com.example.redchina.service.SysInteractionService;

import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 互动关卡控制器（区分用户端和管理端接口）
 */
@RestController
@RequestMapping("/interaction")
public class SysInteractionController {

    @Resource
    private SysInteractionService interactionService;

    // ---------------------- 用户端接口（无需登录，剧情互动时调用）----------------------
    /**
     * 根据剧情节点ID获取互动内容（核心接口：剧情节点触发互动）
     */
    @GetMapping("/node/{storyNodeId}")
    public ResultVo<?> getInteractionByNode(@PathVariable Long storyNodeId) {
        SysInteraction interaction = interactionService.getByStoryNodeId(storyNodeId);
        if (interaction == null) {
            return ResultVo.success(null, "该节点无互动内容");
        }
        return ResultVo.success(interaction);
    }

    // ---------------------- 管理端接口（需ADMIN角色）----------------------
    /**
     * 分页查询所有互动关卡（管理员管理）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/page")
    public ResultVo<?> getInteractionPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String interactionType) {

        IPage<SysInteraction> page = new Page<>(pageNum, pageSize);
        // 条件查询（支持按互动类型筛选）
        IPage<SysInteraction> interactionPage = interactionService.lambdaQuery()
                .eq(interactionType != null, SysInteraction::getInteractionType, interactionType)
                .page(page);

        return ResultVo.success(interactionPage);
    }

    /**
     * 新增互动关卡（管理员操作）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public ResultVo<?> addInteraction(@RequestBody SysInteraction interaction) {
        try {
            boolean success = interactionService.addInteraction(interaction);
            return success ? ResultVo.success(null, "新增互动成功") : ResultVo.error("新增互动失败");
        } catch (IllegalArgumentException e) {
            return ResultVo.error(e.getMessage());
        } catch (RuntimeException e) {
            return ResultVo.error(e.getMessage());
        }
    }

    /**
     * 编辑互动关卡（管理员操作）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/edit")
    public ResultVo<?> editInteraction(@RequestBody SysInteraction interaction) {
        try {
            boolean success = interactionService.updateInteraction(interaction);
            return success ? ResultVo.success(null, "编辑互动成功") : ResultVo.error("编辑互动失败");
        } catch (IllegalArgumentException e) {
            return ResultVo.error(e.getMessage());
        } catch (RuntimeException e) {
            return ResultVo.error(e.getMessage());
        }
    }

    /**
     * 删除互动关卡（管理员操作）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public ResultVo<?> deleteInteraction(@PathVariable Long id) {
        boolean success = interactionService.removeById(id);
        return success ? ResultVo.success(null, "删除互动成功") : ResultVo.error("删除互动失败");
    }

    /**
     * 批量新增互动关卡（管理员初始化剧情时调用）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/batchAdd")
    public ResultVo<?> batchAddInteraction(@RequestBody List<SysInteraction> interactionList) {
        try {
            int count = interactionService.batchAddInteraction(interactionList);
            return ResultVo.success(count, "批量新增成功，共新增" + count + "条互动");
        } catch (Exception e) {
            return ResultVo.error("批量新增失败：" + e.getMessage());
        }
    }
}
