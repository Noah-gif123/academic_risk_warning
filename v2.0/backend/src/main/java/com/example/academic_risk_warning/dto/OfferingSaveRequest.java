package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 新增 / 修改开课实例 */
@Data
public class OfferingSaveRequest {
    private Long id;
    @NotNull(message = "课程不能为空")
    private Long courseId;
    @NotNull(message = "学期不能为空")
    private Long termId;
    @NotNull(message = "计划周数不能为空")
    @Min(value = 1, message = "计划周数至少为 1")
    private Integer plannedWeeks;
    @NotNull(message = "每周课次不能为空")
    @Min(value = 1, message = "每周课次至少为 1")
    private Integer plannedTimesPerWeek;
    @NotNull(message = "及格线不能为空")
    private Integer passScore;
    private String remark;
}
