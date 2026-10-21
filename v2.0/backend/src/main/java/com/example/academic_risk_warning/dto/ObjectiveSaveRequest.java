package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 整批保存教学目标（以提交的清单为准：多退少补） */
@Data
public class ObjectiveSaveRequest {
    @NotNull(message = "教学班不能为空")
    private Long teachingClassId;
    private List<ObjectiveItemDTO> items;
}
