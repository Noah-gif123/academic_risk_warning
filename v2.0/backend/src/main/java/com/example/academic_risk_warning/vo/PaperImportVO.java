package com.example.academic_risk_warning.vo;

import com.example.academic_risk_warning.dto.QuestionDTO;
import lombok.Data;

import java.util.List;

/** 导入结果：题目先落成草稿供教师校对，不直接入库 */
@Data
public class PaperImportVO {
    private String fileName;
    private Integer parsedCount;
    private Integer warningCount;
    private List<String> warnings;
    private List<QuestionDTO> questions;
}
