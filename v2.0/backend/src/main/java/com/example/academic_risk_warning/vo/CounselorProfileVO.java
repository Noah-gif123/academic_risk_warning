package com.example.academic_risk_warning.vo;

import lombok.Data;

/** 导员个人中心 */
@Data
public class CounselorProfileVO {
    private Long counselorId;
    private String counselorNo;
    private String counselorName;
    private String collegeName;
    private String phone;
    private String email;
    private String accountNo;
    private Integer classCount;
}
