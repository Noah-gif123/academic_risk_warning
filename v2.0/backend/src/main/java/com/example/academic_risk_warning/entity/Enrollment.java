package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 选课表（学生×教学班×学期）
 * <p>对应表：enrollment</p>
 */
@Data
@TableName("enrollment")
public class Enrollment {

    /** 选课ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 学生ID */
    private Long studentId;

    /** 教学班ID（决定任课老师与所属行政班范围） */
    private Long teachingClassId;

    /** 开课ID（由 teaching_class 决定、写入时校验一致；用于覆盖率/成绩按开课汇总） */
    private Long offeringId;

    /** 学期ID */
    private Long termId;

    /** 状态：ACTIVE在读/REPEAT重修/WITHDRAWN已退课/EXEMPT免修 */
    private String status;

    /** 重修次数（0=首次修读，1=第一次重修…） */
    private Integer retakeTimes;

    /** 选课/录入时间 */
    private LocalDateTime enrollTime;

    /** 退课时间 */
    private LocalDateTime withdrawTime;

    /** 退课原因 */
    private String withdrawReason;

    /** 退课批准人（教务ID） */
    private Long withdrawApproveBy;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
