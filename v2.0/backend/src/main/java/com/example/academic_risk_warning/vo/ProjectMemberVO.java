package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 组员（含组长标记、贡献占比与折算后的个人得分） */
@Data
public class ProjectMemberVO {
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Boolean leader;
    private BigDecimal ratio;
    private BigDecimal personalScore;
    private String note;
}
