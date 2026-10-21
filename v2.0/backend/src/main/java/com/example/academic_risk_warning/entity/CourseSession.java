package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 课次表（一次课一行；点名与覆盖率的基础）
 * <p>对应表：course_session</p>
 */
@Data
@TableName("course_session")
public class CourseSession {

    /** 课次ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 开课ID（由 teaching_class 派生，冗余用于按开课汇总） */
    private Long offeringId;

    /** 教学班ID（课次与点名按教学班进行） */
    private Long teachingClassId;

    /** 学期ID */
    private Long termId;

    /** 教学周次（对应 term_calendar.week_no） */
    private Integer weekNo;

    /** 本教学班第几次课（1..planned_sessions，课表驱动定位用） */
    private Integer sessionNo;

    /** 上课日期 */
    private LocalDate sessionDate;

    /** 开始时间，如 08:00 */
    private String startTime;

    /** 结束时间，如 09:40 */
    private String endTime;

    /** 上课地点 */
    private String location;

    /** 课型：NORMAL普通/LAB实验/EXAM考试/OTHER */
    private String sessionType;

    /** 课次状态：NORMAL正常/CANCELLED停课/ADJUSTED已调课/MERGED合并 */
    private String status;

    /** 停课/调课说明（如 国庆放假、校运动会） */
    private String adjustNote;

    /** 点名状态：NOT_CALLED未点名/CALLED已点名/PARTIAL部分点名 */
    private String rollCallStatus;

    /** 最近一次点名提交时间 */
    private LocalDateTime rollCallTime;

    /** 点名操作人（教师ID） */
    private Long rollCallBy;

    /** 该课次产生的考勤记录条数（=0 表示未点名） */
    private Integer rollCallCount;

    /** 是否作废（重排课/误录）：1作废，不计入任何分母 */
    private Integer isVoid;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
