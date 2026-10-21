package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 新增 / 修改教学班 */
@Data
public class TeachingClassSaveRequest {
    private Long id;
    @NotNull(message = "开课实例不能为空")
    private Long offeringId;
    @NotNull(message = "任课教师不能为空")
    private Long teacherId;
    @NotBlank(message = "班级编码不能为空")
    private String classCode;
    private String className;
    private Integer capacity;
    private Integer isRetakeClass;
    /** 关联的行政班（用于一键把学生选进本教学班） */
    private List<Long> adminClassIds;
}
