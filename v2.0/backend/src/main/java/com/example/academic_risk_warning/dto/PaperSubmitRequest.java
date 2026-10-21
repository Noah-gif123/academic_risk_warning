package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 交卷：一次性提交全部作答（作业与测验不允许迟交、不允许教师代交） */
@Data
public class PaperSubmitRequest {
    private List<AnswerSubmitDTO> answers;
    /** 客户端记录的进入时间，用于算用时（仅参考，超时以服务端为准） */
    private LocalDateTime startedAt;
}
