package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 课程导学（每位教师一份）
 * <p>对应表：course_guide</p>
 */
@Data
@TableName("course_guide")
public class CourseGuide {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 教学班ID（一位老师一份） */
    private Long teachingClassId;

    /** 开课ID（冗余，便于按开课查询） */
    private Long offeringId;

    /** 学期ID */
    private Long termId;

    /** 导学正文（HTML 富文本，内容随意） */
    private String content;

    /** 正文格式：HTML/TEXT */
    private String contentFormat;

    /** 版本号（每次保存 +1） */
    private Integer version;

    /** 是否已发布给学生：1是 */
    private Integer isPublished;

    /** 发布时间 */
    private LocalDateTime publishTime;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
