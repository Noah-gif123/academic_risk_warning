package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 教师表
 * <p>对应表：teacher</p>
 */
@Data
@TableName("teacher")
public class Teacher {

    /** 教师ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 账号ID */
    private Long accountId;

    /** 工号 */
    private String teacherNo;

    /** 姓名 */
    private String teacherName;

    /** 性别：1男 2女 0未知 */
    private Integer gender;

    /** 职称：教授/副教授/讲师/助教/未定级（展示用；Δ 收缩由样本量自动决定，不依赖职称） */
    private String title;

    /** 所属学院/教研室 */
    private String collegeName;

    private String phone;

    private String email;

    /** 状态：ACTIVE/LEFT离职 */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
