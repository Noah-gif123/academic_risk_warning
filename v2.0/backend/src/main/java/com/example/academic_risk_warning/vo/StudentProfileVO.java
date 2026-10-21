package com.example.academic_risk_warning.vo;

import lombok.Data;

/** 学生个人中心 */
@Data
public class StudentProfileVO {
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Integer gender;
    private Integer enrollYear;
    private String majorName;
    private String className;
    private String phone;
    private String email;
    private String accountNo;
}
