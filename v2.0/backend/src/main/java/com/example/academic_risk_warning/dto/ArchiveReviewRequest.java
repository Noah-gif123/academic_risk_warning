package com.example.academic_risk_warning.dto;

import lombok.Data;

/** 教务审核归档：通过 / 退回 */
@Data
public class ArchiveReviewRequest {
    private String note;
    private String reason;
}
