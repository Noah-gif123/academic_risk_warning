package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 教学班-行政班关联表
 * <p>对应表：teaching_class_admin_class</p>
 */
@Data
@TableName("teaching_class_admin_class")
public class TeachingClassAdminClass {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 教学班ID */
    private Long teachingClassId;

    /** 行政班ID */
    private Long adminClassId;

    private LocalDateTime createTime;
}
