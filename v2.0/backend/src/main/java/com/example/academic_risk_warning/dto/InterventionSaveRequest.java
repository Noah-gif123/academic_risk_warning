package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 导员录入干预记录 */
@Data
public class InterventionSaveRequest {
    @NotNull(message = "学生不能为空")
    private Long studentId;
    /** TALK 谈话 / MEETING 班会 / PARENT 联系家长 / OTHER 其他 */
    private String actionType;
    private String content;
    private String result;
    /** 处理是否有效：EFFECTIVE / INEFFECTIVE / PENDING */
    private String effective;
    private String remark;
}
