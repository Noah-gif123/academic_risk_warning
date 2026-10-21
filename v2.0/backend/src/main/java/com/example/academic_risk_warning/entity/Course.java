package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 课程（科目）表
 * <p>对应表：course</p>
 */
@Data
@TableName("course")
public class Course {

    /** 课程ID（科目） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 课程名称 */
    private String courseName;

    /** 课程类型：只分两类 —— EXAM考试课 / ASSESS考查课（上机/卷面、个人/团队项目等差异不单独分类，改由 difficulty_factor_code 指向课型难度因子档位） */
    private String courseType;

    /** 学分 */
    private BigDecimal credit;

    /** 计划学时（展示用；课次口径以开课为准） */
    private Integer totalHours;

    /** 先修课程ID（同科目序列，如 高数1→高数2→高数3） */
    private Long prerequisiteCourseId;

    /** 开课学院 */
    private String collegeName;

    /** 课型难度因子档位编码（教务按课指定，如 PAPER卷面/MACHINE上机/PERSONAL个人项目/TEAM团队项目/OTHER；留空则用所属 course_type 的默认档）。档位与数值定义在 risk_engine_config.course_type_factor_json，可随时增档而不用改建表 */
    private String difficultyFactorCode;

    /** 状态：ACTIVE/CLOSED停开 */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
