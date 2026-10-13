package com.example.redchina.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.redchina.entity.SysInteractionDetail;
import com.example.redchina.mapper.SysInteractionDetailMapper;
import com.example.redchina.service.SysInteractionDetailService;
import com.example.redchina.common.ResultVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;




@Service
public class SysInteractionDetailServiceImpl extends ServiceImpl<SysInteractionDetailMapper, SysInteractionDetail> implements SysInteractionDetailService {

    @Resource
    private SysInteractionDetailMapper interactionDetailMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public SysInteractionDetail getByInteractionId(Long interactionId) {
        return interactionDetailMapper.selectByInteractionId(interactionId);
    }

    @Override
    public ResultVo<?> submitQuestionAnswer(Long interactionId, Integer optionId) {
        // 1. 获取互动详情
        SysInteractionDetail detail = getByInteractionId(interactionId);
        if (detail == null) {
            return ResultVo.error("互动关卡不存在");
        }
        if (!"QUESTION".equals(detail.getType())) {
            return ResultVo.error("该互动类型不是问答");
        }

        // 2. 校验答案（correctAnswer存储的是正确选项ID）
        boolean isCorrect = detail.getCorrectAnswer().equals(optionId.toString());
        if (isCorrect) {
            return ResultVo.success("回答正确！");
        } else {
            return ResultVo.error("回答错误，请重试！");
        }
    }
}
