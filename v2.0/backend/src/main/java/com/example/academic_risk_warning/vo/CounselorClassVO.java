package com.example.academic_risk_warning.vo;

import lombok.Data;

/** 导员带班视图 */
@Data
public class CounselorClassVO {
    private Long id;
    private Long termId;
    private Long counselorId;
    private String counselorNo;
    private String counselorName;
    private Long adminClassId;
    private String classCode;
    private String className;
    private Integer gradeYear;
    private Integer studentCount;
    private Integer isPrimary;
}
