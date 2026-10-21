package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 导员-行政班关联表
 * <p>对应表：counselor_class</p>
 */
@Data
@TableName("counselor_class")
public class CounselorClass {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 导员ID */
    private Long counselorId;

    /** 行政班ID */
    private Long adminClassId;

    /** 学期ID（同班换导员时按学期留痕） */
    private Long termId;

    /** 是否主责导员：1主 0副（副导员可多人） */
    private Integer isPrimary;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
