package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 导员表
 * <p>对应表：counselor</p>
 */
@Data
@TableName("counselor")
public class Counselor {

    /** 导员ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 账号ID */
    private Long accountId;

    /** 工号 */
    private String counselorNo;

    /** 姓名 */
    private String counselorName;

    /** 性别：1男 2女 0未知 */
    private Integer gender;

    /** 所属学院 */
    private String collegeName;

    private String phone;

    private String email;

    /** 状态：ACTIVE/LEFT离职 */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
