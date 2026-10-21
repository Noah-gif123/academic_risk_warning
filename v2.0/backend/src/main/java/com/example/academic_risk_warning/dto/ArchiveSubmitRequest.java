package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 教师提交归档（期中 / 期末） */
@Data
public class ArchiveSubmitRequest {
    @NotNull(message = "教学班不能为空")
    private Long teachingClassId;
    /** MID / FINAL */
    @NotBlank(message = "归档阶段不能为空")
    private String phase;
    private String note;
}
