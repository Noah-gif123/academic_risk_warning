package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 新建 / 保存项目任务 */
@Data
public class ProjectSaveRequest {
    private Long id;
    @NotNull(message = "教学班不能为空")
    private Long teachingClassId;
    @NotBlank(message = "项目名称不能为空")
    private String projectName;
    private String description;
    private Long lessonId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 1=团队项目（需组长提交贡献表） */
    private Integer isTeam;
    private BigDecimal fullScore;
    /** 允许的提交形式，逗号分隔：WORD,PDF,CODE,PPT,ZIP,LINK */
    private String allowFormats;
    private Long schemeItemId;
}
