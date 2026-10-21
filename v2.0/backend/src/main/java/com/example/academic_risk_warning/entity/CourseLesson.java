package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 目录课时表（一行=一次课）
 * <p>对应表：course_lesson</p>
 */
@Data
@TableName("course_lesson")
public class CourseLesson {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 教学班ID */
    private Long teachingClassId;

    /** 开课ID */
    private Long offeringId;

    /** 学期ID */
    private Long termId;

    /** 课次ID（course_session，一次课一行） */
    private Long sessionId;

    /** 第几周（冗余，便于展示与排序） */
    private Integer weekNo;

    /** 第几次课（冗余） */
    private Integer sessionNo;

    /** 本次课要讲的内容标题（可含多条，用分号连接） */
    private String title;

    /** 目录顺序 */
    private Integer sortNo;

    /** 状态：ACTIVE/DISABLED */
    private String status;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
