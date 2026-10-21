package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 开课实例表（计划/实际课次、及格线）
 * <p>对应表：course_offering</p>
 */
@Data
@TableName("course_offering")
public class CourseOffering {

    /** 开课ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 课程ID */
    private Long courseId;

    /** 学期ID */
    private Long termId;

    /** 课程组ID */
    private Long groupId;

    /** 计划教学周数（教务开学前导入） */
    private Integer plannedWeeks;

    /** 计划每周课次（教务开学前导入） */
    private Integer plannedTimesPerWeek;

    /** 计划总课次 = 计划周数 × 每周课次（教务锁定后不变） */
    private Integer plannedSessions;

    /** 实际教学周数（老师期末申报：因节假日调休等的真实值） */
    private Integer actualWeeks;

    /** 实际总课次（老师期末申报） */
    private Integer actualSessions;

    /** 实际课次申报状态：NONE未申报/PENDING待教务确认/CONFIRMED已确认/RETURNED已退回 */
    private String actualSubmitStatus;

    /** 申报人（教师ID） */
    private Long actualSubmitBy;

    /** 申报时间 */
    private LocalDateTime actualSubmitTime;

    /** 申报说明（如：国庆放假一周、校运会停课一次） */
    private String actualSubmitNote;

    /** 确认人（教务ID） */
    private Long actualConfirmBy;

    /** 确认时间 */
    private LocalDateTime actualConfirmTime;

    /** 判定用分母课次：未确认前=计划课次，确认后=实际课次（1/3 门槛与覆盖率共用） */
    private Integer effectiveSessions;

    /** 分母来源：PLANNED计划/ACTUAL教务确认后的实际课次 */
    private String effectiveSource;

    /** 及格线（本学期该课适用） */
    private Integer passScore;

    /** 状态：PLANNING筹备/ONGOING进行中/FINISHED已结束/ARCHIVED已归档 */
    private String status;

    private String remark;

    /** 创建人（教务ID） */
    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
