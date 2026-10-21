package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 保存成绩构成（草稿） */
@Data
public class ScoreSchemeSaveRequest {
    @NotNull(message = "开课实例不能为空")
    private Long offeringId;
    private String level;
    private Long sourceTemplateId;
    private String remark;
    private List<ScoreItemDTO> items;
}
