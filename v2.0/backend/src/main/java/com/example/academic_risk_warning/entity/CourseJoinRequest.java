package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 入课申请表（学生申请/教师审核/教务知会）
 * <p>对应表：course_join_request</p>
 */
@Data
@TableName("course_join_request")
public class CourseJoinRequest {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 教学班ID（申请加入的班） */
    private Long teachingClassId;

    /** 开课ID（冗余） */
    private Long offeringId;

    /** 学期ID */
    private Long termId;

    /** 学生ID */
    private Long studentId;

    /** 学生输入的邀请码（留档，便于纠纷追溯） */
    private String inviteCode;

    /** 申请来源：INVITE_CODE邀请码/ADMIN教务导入 */
    private String applySource;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 状态：APPLIED待审/APPROVED已同意/REJECTED已拒绝/AUTO免审入班 */
    private String status;

    /** 审核人（教师ID；AUTO 时为空） */
    private Long reviewBy;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 审核意见 / 拒绝原因 */
    private String reviewNote;

    /** 是否已向教务发出知会：1是 */
    private Integer notifyAdmin;

    /** 知会时间 */
    private LocalDateTime notifyTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
