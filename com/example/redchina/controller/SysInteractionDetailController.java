package com.example.redchina.controller;

import com.example.redchina.entity.SysInteractionDetail;
import com.example.redchina.common.ResultVo;
import com.example.redchina.service.SysInteractionDetailService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 互动关卡详情控制器（问答/小游戏交互）
 */
@RestController
@RequestMapping("/interaction/detail")
public class SysInteractionDetailController {

    @Resource
    private SysInteractionDetailService interactionDetailService;

    /**
     * 获取互动关卡详情
     */
    @GetMapping("/{interactionId}")
    public ResultVo<SysInteractionDetail> getDetail(@PathVariable Long interactionId) {
        SysInteractionDetail detail = interactionDetailService.getByInteractionId(interactionId);
        return ResultVo.success(detail);
    }

    /**
     * 提交问答答案
     */
    @PostMapping("/submit-question")
    public ResultVo<?> submitQuestion(
            @RequestParam Long interactionId,
            @RequestParam Integer optionId) {
        return interactionDetailService.submitQuestionAnswer(interactionId, optionId);
    }
}
