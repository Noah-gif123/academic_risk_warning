package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 新增 / 修改专业 */
@Data
public class MajorSaveRequest {
    private Long id;
    @NotBlank(message = "专业编码不能为空")
    private String majorCode;
    @NotBlank(message = "专业名称不能为空")
    private String majorName;
    private String collegeCode;
    private String collegeName;
}
