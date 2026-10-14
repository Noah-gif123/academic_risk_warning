package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 学生出勤信息
 */
@Data
@TableName("attendance")
public class Attendance {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生ID */
    private Long studentId;

    /** 课程ID */
    private Long courseId;

    /** 总课时 */
    private Integer totalHours;

    /** 缺勤次数 */
    private Integer absentCount;

    /** 迟到次数 */
    private Integer lateCount;

    /** 出勤率（百分比） */
    private java.math.BigDecimal attendanceRate;
}
