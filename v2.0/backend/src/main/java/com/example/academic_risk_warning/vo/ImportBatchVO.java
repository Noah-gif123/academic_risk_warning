package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 导入批次（可整批回滚） */
@Data
public class ImportBatchVO {
    private Long id;
    private String batchNo;
    private String importType;
    private String fileName;
    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;
    private String status;
    private String statusText;
    private String operatorName;
    private String operatorRole;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime rollbackTime;
    private String rollbackReason;
    private String remark;
    /** 失败明细（最多前 10 条） */
    private java.util.List<String> errors;
}
