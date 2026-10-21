package com.example.academic_risk_warning.dto;

import lombok.Data;

/** 点名单中的一行：学生 + 考勤状态 */
@Data
public class AttendanceItemDTO {
    private Long studentId;
    /** PRESENT / LATE / ABSENT / LEAVE */
    private String status;
    /** PERSONAL / SICK / OFFICIAL（仅请假时填） */
    private String leaveType;
}
