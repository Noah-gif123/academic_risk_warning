package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.util.List;

/** 批量导入点名单的结果 */
@Data
public class AttendanceImportVO {
    private String fileName;
    private Integer totalRows;
    private Integer successRows;
    private Integer skippedRows;
    private List<String> errors;
}
