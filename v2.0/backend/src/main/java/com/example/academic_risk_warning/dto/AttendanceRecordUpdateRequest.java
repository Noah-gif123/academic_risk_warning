package com.example.academic_risk_warning.dto;

import lombok.Data;

/** 单行修改（平时可直接改；归档后只有教务能改，且必须写原因） */
@Data
public class AttendanceRecordUpdateRequest {
    private String status;
    private String leaveType;
    private String note;
}
