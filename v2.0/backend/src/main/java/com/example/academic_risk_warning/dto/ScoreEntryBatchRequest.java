package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 批量录入某个手工评分项 */
@Data
public class ScoreEntryBatchRequest {
    @NotNull(message = "教学班不能为空")
    private Long teachingClassId;
    @NotNull(message = "评分项不能为空")
    private Long schemeItemId;
    private Integer timesNo;
    private List<ScoreEntryDTO> items;
}
