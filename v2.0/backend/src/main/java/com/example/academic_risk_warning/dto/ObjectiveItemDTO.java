package com.example.academic_risk_warning.dto;

import lombok.Data;

/** 教学目标（一行） */
@Data
public class ObjectiveItemDTO {
    private Long id;
    private String objCode;
    private String objContent;
    private String objType;
    private Integer sortNo;
}
