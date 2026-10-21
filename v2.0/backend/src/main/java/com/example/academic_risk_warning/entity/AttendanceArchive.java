package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 考勤归档批次表（期中/期末，双轨制的落点）
 * <p>对应表：attendance_archive</p>
 */
@Data
@TableName("attendance_archive")
public class AttendanceArchive {

    /** 归档批次ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 开课ID */
    private Long offeringId;

    /** 教学班ID（老师按教学班提交；教务按课程汇总查看） */
    private Long teachingClassId;

    /** 提交老师ID */
    private Long teacherId;

    /** 学期ID */
    private Long termId;

    /** 归档阶段：MID期中/FINAL期末 */
    private String phase;

    /** 归档窗口开启时间 */
    private LocalDateTime windowOpenTime;

    /** 提交截止时间 */
    private LocalDateTime deadline;

    /** 老师提交时间 */
    private LocalDateTime submitTime;

    /** 提交人（教师ID） */
    private Long submitBy;

    /** 提交说明 */
    private String submitNote;

    /** 本次归档覆盖的起始课次序号 */
    private Integer sessionFrom;

    /** 本次归档覆盖的结束课次序号 */
    private Integer sessionTo;

    /** 本次归档覆盖课次数 */
    private Integer sessionCount;

    /** 本次归档涉及考勤记录条数 */
    private Integer recordCount;

    /** 提交时的点名覆盖率(%)：已点名课次/应完成课次 */
    private BigDecimal coverageRate;

    /** 状态：PENDING待提交/SUBMITTED已提交待审核/REVIEWED已确认/RETURNED已退回 */
    private String status;

    /** 审核人（教务ID） */
    private Long reviewBy;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 审核意见 */
    private String reviewNote;

    /** 退回操作人（教务ID） */
    private Long returnBy;

    /** 退回时间 */
    private LocalDateTime returnTime;

    /** 退回原因 */
    private String returnReason;

    /** 催办次数（未提交时红点+站内信催办） */
    private Integer remindCount;

    /** 最近催办时间 */
    private LocalDateTime lastRemindTime;

    /** 是否作废（重开批次等） */
    private Integer isVoid;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
