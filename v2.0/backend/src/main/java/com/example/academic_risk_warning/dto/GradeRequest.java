package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.util.List;

/** 提交主观题批改 */
@Data
public class GradeRequest {
    private List<GradeItemDTO> items;
    /** 整卷批注 */
    private String remark;
}
