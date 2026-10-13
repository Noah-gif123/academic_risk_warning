package com.example.redchina.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.redchina.entity.SysInteractionDetail;
import com.example.redchina.common.ResultVo;

public interface SysInteractionDetailService extends IService<SysInteractionDetail> {
    // 根据互动关卡ID获取详情
    SysInteractionDetail getByInteractionId(Long interactionId);

    // 提交问答答案校验
    ResultVo<?> submitQuestionAnswer(Long interactionId, Integer optionId);
}
