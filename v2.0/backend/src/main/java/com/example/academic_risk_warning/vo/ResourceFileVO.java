package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资料库文件（课程组共享视图）。
 *
 * <p>资源文件按「开课实例（同一门课）」共享：同一课程组内不同教学班、不同老师上传的资料互相可见；
 * 列表里带上上传人与所属教学班，便于区分"谁传的、传给了哪个班"。</p>
 */
@Data
public class ResourceFileVO {
    private Long id;
    private String fileName;
    private String fileExt;
    private Long fileSize;
    private String mimeType;

    private Long offeringId;
    private Long teachingClassId;
    /** 上传时所在教学班编号，如 CS-01-A */
    private String classCode;

    private Long uploadBy;
    private String uploaderName;
    private String uploadRole;
    private LocalDateTime createTime;

    /** 是否当前登录老师上传的（决定能否作废） */
    private Boolean isMine;
}
