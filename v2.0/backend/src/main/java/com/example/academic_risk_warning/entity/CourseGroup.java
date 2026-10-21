package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 课程组表（课程×学期，无组长）
 * <p>对应表：course_group</p>
 */
@Data
@TableName("course_group")
public class CourseGroup {

    /** 课程组ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 课程ID（科目） */
    private Long courseId;

    /** 学期ID */
    private Long termId;

    /** 构成层级：L1只填三项总分/L2平时拆项(默认)/L3自定义项(实验、三四级项目等) */
    private String level;

    /** 申请的层级（老师发起升级时填写） */
    private String levelRequest;

    /** 升级申请状态：NONE无/PENDING待教务审批/APPROVED已批准/REJECTED已驳回 */
    private String levelRequestStatus;

    /** 申请人（教师ID） */
    private Long levelRequestBy;

    /** 申请时间 */
    private LocalDateTime levelRequestTime;

    /** 申请理由（如：本课含 5 次实验 + 三级项目） */
    private String levelRequestReason;

    /** 审批人（教务ID） */
    private Long levelApproveBy;

    /** 审批时间 */
    private LocalDateTime levelApproveTime;

    /** 审批意见 */
    private String levelApproveNote;

    /** 状态：ACTIVE/CLOSED */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
