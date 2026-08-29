package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exercise")
public class Exercise {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teacherId;
    private Long courseId;
    private Long kpId;
    private String title;
    private String description;
    private String fileUrl;
    private String fileType;
    private String difficulty;
    private String questionType;
    private String answer;
    private Integer isActive;
    /** 审核状态: PENDING(待审核) / APPROVED(已通过) / REJECTED(已驳回) */
    private String auditStatus;
    /** 审核备注 */
    private String auditRemark;
    private LocalDateTime createTime;
}
