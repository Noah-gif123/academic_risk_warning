package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.util.List;

/** 选课：按学生 ID 批量加入教学班 */
@Data
public class EnrollRequest {
    private List<Long> studentIds;
}
