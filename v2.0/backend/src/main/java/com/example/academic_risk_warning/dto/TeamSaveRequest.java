package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.util.List;

/** 组队：本人为组长，提交成员名单 */
@Data
public class TeamSaveRequest {
    private String teamName;
    private List<Long> memberIds;
}
