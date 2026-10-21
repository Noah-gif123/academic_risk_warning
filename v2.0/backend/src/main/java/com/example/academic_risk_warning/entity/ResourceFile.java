package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 资源文件表（课程资料库）
 * <p>对应表：resource_file</p>
 */
@Data
@TableName("resource_file")
public class ResourceFile {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属教学班ID（空=教师公共资源） */
    private Long teachingClassId;

    /** 开课ID（空=公共资源） */
    private Long offeringId;

    /** 学期ID（公共资源可为空） */
    private Long termId;

    /** 原始文件名（含扩展名） */
    private String fileName;

    /** 存储路径或对象 Key */
    private String filePath;

    /** 扩展名：pdf/ppt/pptx/doc/docx/xlsx/zip/mp4… */
    private String fileExt;

    /** 文件大小（字节） */
    private Long fileSize;

    /** MIME 类型 */
    private String mimeType;

    /** 存储方式：LOCAL本地目录/OSS对象存储 */
    private String storageType;

    /** 上传人（教师ID） */
    private Long uploadBy;

    /** 上传人角色 */
    private String uploadRole;

    private LocalDateTime createTime;

    /** 是否作废：1是（逻辑删除） */
    private Integer isVoid;

    /** 作废原因 */
    private String voidReason;
}
