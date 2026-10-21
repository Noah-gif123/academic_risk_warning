package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 教学周历表（周次/假期/考试周）
 * <p>对应表：term_calendar</p>
 */
@Data
@TableName("term_calendar")
public class TermCalendar {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 学期ID */
    private Long termId;

    /** 教学周次（1..total_weeks） */
    private Integer weekNo;

    /** 本周起始日期（周一） */
    private LocalDate startDate;

    /** 本周结束日期（周日） */
    private LocalDate endDate;

    /** 周类型：NORMAL正常/HOLIDAY假期/EXAM考试周/PRACTICE实践周 */
    private String weekType;

    /** 本周内法定假日天数（用于扣减应上课次） */
    private Integer holidayDays;

    /** 备注，如 国庆假期/校运动会 */
    private String note;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
