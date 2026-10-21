package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.util.List;

/** 组长提交贡献表（提交后锁定，教师也不能改） */
@Data
public class ContributionSubmitRequest {
    private List<ContributionItemDTO> items;
    private String remark;
}
