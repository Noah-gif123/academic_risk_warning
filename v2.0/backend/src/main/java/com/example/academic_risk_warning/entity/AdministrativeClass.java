package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 行政班表
 * <p>对应表：administrative_class</p>
 */
@Data
@TableName("administrative_class")
public class AdministrativeClass {

    /** 行政班ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 班级编码，如 SOFT-2024-1 */
    private String classCode;

    /** 班级名称，如 软件1班 */
    private String className;

    /** 所属专业ID */
    private Long majorId;

    /** 年级（入学年份），如 2024 */
    private Integer gradeYear;

    /** 状态：ACTIVE/GRADUATED/DISMISSED */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
