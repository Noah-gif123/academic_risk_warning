package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 新增 / 修改行政班 */
@Data
public class AdminClassSaveRequest {
    private Long id;
    @NotBlank(message = "班级编码不能为空")
    private String classCode;
    @NotBlank(message = "班级名称不能为空")
    private String className;
    @NotNull(message = "所属专业不能为空")
    private Long majorId;
    @NotNull(message = "年级不能为空")
    private Integer gradeYear;
}
