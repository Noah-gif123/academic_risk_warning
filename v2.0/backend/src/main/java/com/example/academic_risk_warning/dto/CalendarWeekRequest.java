package com.example.academic_risk_warning.dto;

import lombok.Data;

/** 修改某一周的周历类型（教学周 / 假期 / 考试周…） */
@Data
public class CalendarWeekRequest {
    private String weekType;
    private Integer holidayDays;
    private String note;
}
