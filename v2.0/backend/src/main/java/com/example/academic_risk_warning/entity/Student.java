package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 学生表
 * <p>对应表：student</p>
 */
@Data
@TableName("student")
public class Student {

    /** 学生ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 账号ID（迁移期可为空，迁移后应非空） */
    private Long accountId;

    /** 学号 */
    private String studentNo;

    /** 姓名 */
    private String studentName;

    /** 性别：1男 2女 0未知 */
    private Integer gender;

    /** 入学年份，如 2024（年级由此推算，不再存"大一上"这类学期值） */
    private Integer enrollYear;

    /** 专业ID（转专业后更新，历史由 audit_log 留痕） */
    private Long majorId;

    /** 行政班ID */
    private Long adminClassId;

    /** 学籍状态：IN_SCHOOL在读/SUSPENDED休学/WITHDRAWN退学/GRADUATED毕业 */
    private String status;

    private String phone;

    private String email;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
