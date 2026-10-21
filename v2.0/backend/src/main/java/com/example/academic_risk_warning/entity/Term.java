package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 学期表（全库时间轴）
 * <p>对应表：term</p>
 */
@Data
@TableName("term")
public class Term {

    /** 学期ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 学期编码，如 2024-2025-1 */
    private String termCode;

    /** 学年，如 2024-2025 */
    private String schoolYear;

    /** 学期序号：1=秋季 2=春季 3=小学期 */
    private Integer termNo;

    /** 学期显示名，如 2024-2025学年第一学期 */
    private String termName;

    /** 开学日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 教学周数（默认值，具体以各开课为准） */
    private Integer totalWeeks;

    /** 是否当前学期：1是 0否（全库只允许一条为1） */
    private Integer isCurrent;

    /** 状态：PLANNING筹备中/ACTIVE进行中/CLOSED已结束 */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
