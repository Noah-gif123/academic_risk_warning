package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.util.List;

/** 审计日志分页 */
@Data
public class AuditLogPageVO {
    private List<AuditLogVO> items;
    private Integer total;
    private Integer page;
    private Integer size;
    private Integer pages;
    private List<String> bizTypes;
    private List<String> actions;
}
