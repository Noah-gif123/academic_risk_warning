// src/main/java/com/example/redchina/controller/InteractionController.java
package com.example.redchina.controller;

import com.example.redchina.common.ResultVo;
import com.example.redchina.entity.SysInteraction;
import com.example.redchina.entity.SysStoryNode;
import com.example.redchina.service.SysInteractionService;
import com.example.redchina.service.SysStoryNodeService;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/interaction")
public class InteractionController {

    @Resource
    private SysInteractionService interactionService;
    @Resource
    private SysStoryNodeService storyNodeService;

    // 获取节点互动内容
    @GetMapping("/node/{nodeId}")
    public ResultVo<?> getNodeInteraction(@PathVariable Long nodeId) {
        SysInteraction interaction = interactionService.getByStoryNodeId(nodeId);
        return ResultVo.success(interaction);
    }

    // 提交互动结果
    @PostMapping("/submit")
    public ResultVo<?> submitInteraction(@RequestParam Long interactionId,
                                         @RequestParam boolean success) {
        SysInteraction interaction = interactionService.getById(interactionId);
        if (interaction == null) {
            return ResultVo.error("互动内容不存在");
        }
        Long targetNodeId = success ? interaction.getSuccessNodeId() : interaction.getFailNodeId();
        SysStoryNode targetNode = storyNodeService.getById(targetNodeId);
        return ResultVo.success(targetNode);
    }
}
