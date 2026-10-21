package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 一次点名提交（一节课一个批次，默认全班出勤，只提交异常） */
@Data
public class AttendanceCallRequest {
    @NotNull(message = "教学班不能为空")
    private Long teachingClassId;
    @NotNull(message = "课次不能为空")
    private Long sessionId;
    private String note;
    private List<AttendanceItemDTO> items;
}
