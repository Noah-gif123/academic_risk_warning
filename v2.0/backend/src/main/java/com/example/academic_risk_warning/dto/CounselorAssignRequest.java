package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 辅导员分配：把若干行政班分配给某位导员（按学期） */
@Data
public class CounselorAssignRequest {
    @NotNull(message = "学期不能为空")
    private Long termId;
    @NotNull(message = "辅导员不能为空")
    private Long counselorId;
    @NotNull(message = "行政班不能为空")
    private List<Long> adminClassIds;
}
