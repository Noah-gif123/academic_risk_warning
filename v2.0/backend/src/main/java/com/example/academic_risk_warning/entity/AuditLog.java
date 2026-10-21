package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 统一审计日志表
 * <p>对应表：audit_log</p>
 */
@Data
@TableName("audit_log")
public class AuditLog {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 业务类型：SCORE成绩/ATTENDANCE考勤/ARCHIVE归档/REFER上报/INTERVENTION干预/COURSE_GROUP课程组/SCORE_SCHEME成绩构成/OFFERING开课/ACTUAL_SESSIONS实际课次/EXAM_HEADCOUNT考核人数/ACCOUNT账号/NOTIFICATION通知/IMPORT导入/QUESTIONNAIRE问卷/SYSTEM系统 */
    private String bizType;

    /** 业务对象ID */
    private Long bizId;

    /** 动作：CREATE/UPDATE/DELETE/VOID/RESTORE/LOCK/UNLOCK/CONFIRM/APPROVE/REJECT/SUBMIT/RETURN/REFER/TERMINATE/IMPORT/ROLLBACK/LOGIN/PASSWORD_RESET */
    private String action;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人角色：STUDENT/TEACHER/COUNSELOR/ADMIN/SYSTEM */
    private String operatorRole;

    /** 变更前快照（敏感字段脱敏，如密码只记是否变更） */
    private String beforeJson;

    /** 变更后快照 */
    private String afterJson;

    /** 说明（人可读） */
    private String detail;

    /** 来源IP */
    private String ip;

    /** 客户端信息（可选） */
    private String userAgent;

    /** 发生时间 */
    private LocalDateTime createTime;
}
