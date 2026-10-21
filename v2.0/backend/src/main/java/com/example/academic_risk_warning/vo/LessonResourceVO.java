package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 课时资源视图（附带被引用对象的展示信息） */
@Data
public class LessonResourceVO {
    private Long id;
    private Long lessonId;
    private String phase;
    private String resType;
    private Long refId;
    private String extUrl;
    private String title;
    private Integer sortNo;
    private String publishStatus;
    private LocalDateTime publishTime;
    /** 文件资源的原始文件名 / 外链地址（展示用） */
    private String displayName;
    private Long fileSize;
    private String fileExt;
}
