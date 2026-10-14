package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 历史风险与学习稳定性表
 */
@Data
@TableName("history_risk")
public class HistoryRisk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    /** 年级：大一/大二/大三 */
    private String gradeLevel;

    private String lastTermFailed;

    private String studyStable;
}
