package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 在课时的课前 / 课中 / 课后挂一条资源或任务 */
@Data
public class LessonResourceSaveRequest {
    @NotNull(message = "课时不能为空")
    private Long lessonId;
    /** BEFORE / DURING / AFTER */
    @NotNull(message = "阶段不能为空")
    private String phase;
    /** FILE / LINK / TEXT / PAPER / PROJECT */
    @NotNull(message = "资源类型不能为空")
    private String resType;
    private Long refId;
    private String extUrl;
    private String title;
    private Boolean publish;
}
