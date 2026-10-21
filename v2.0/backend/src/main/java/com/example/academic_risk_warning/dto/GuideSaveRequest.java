package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 保存课程导学 */
@Data
public class GuideSaveRequest {
    @NotNull(message = "教学班不能为空")
    private Long teachingClassId;
    private String content;
    /** HTML / TEXT */
    private String contentFormat;
    /** 是否同时发布给学生 */
    private Boolean publish;
}
