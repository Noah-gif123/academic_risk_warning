package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** 新增 / 修改学期 */
@Data
public class TermSaveRequest {
    private Long id;
    @NotBlank(message = "学期编码不能为空")
    private String termCode;
    @NotBlank(message = "学年不能为空")
    private String schoolYear;
    @NotNull(message = "学期序号不能为空")
    private Integer termNo;
    @NotBlank(message = "学期名称不能为空")
    private String termName;
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;
    @NotNull(message = "总周数不能为空")
    private Integer totalWeeks;
}
