package com.example.academic_risk_warning.dto;

import lombok.Data;

/** 教师设置邀请码与是否开启入班审核 */
@Data
public class InviteCodeDTO {
    private String inviteCode;
    private Integer joinReviewEnabled;
}
