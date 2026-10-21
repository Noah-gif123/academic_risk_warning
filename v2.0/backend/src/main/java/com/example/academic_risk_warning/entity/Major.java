package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 专业/学院表
 * <p>对应表：major</p>
 */
@Data
@TableName("major")
public class Major {

    /** 专业ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 专业代码 */
    private String majorCode;

    /** 专业名称 */
    private String majorName;

    /** 学院代码 */
    private String collegeCode;

    /** 学院名称 */
    private String collegeName;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
