package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.util.List;

/** 项目提交：文件（资料库 fileId）+ 仓库链接 + 说明 */
@Data
public class ProjectSubmitRequest {
    private List<Long> fileIds;
    private String repoUrl;
    private String remark;
}
