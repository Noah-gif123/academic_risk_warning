package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 课时资源与任务表（课前/课中/课后）
 * <p>对应表：lesson_resource</p>
 */
@Data
@TableName("lesson_resource")
public class LessonResource {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 课时ID（course_lesson） */
    private Long lessonId;

    /** 教学班ID（冗余） */
    private Long teachingClassId;

    /** 学期ID */
    private Long termId;

    /** 阶段：BEFORE课前/DURING课中/AFTER课后 */
    private String phase;

    /** 资源类型：FILE文件/LINK外链/TEXT富文本/PAPER测验或作业/PROJECT项目 */
    private String resType;

    /** 关联对象ID（FILE→resource_file / PAPER→paper / PROJECT→project_task） */
    private Long refId;

    /** 外链地址（LINK 时使用；视频/网页） */
    private String extUrl;

    /** 展示标题 */
    private String title;

    /** 同组内顺序 */
    private Integer sortNo;

    /** 发布状态：DRAFT草稿/PUBLISHED已发布 */
    private String publishStatus;

    /** 发布时间 */
    private LocalDateTime publishTime;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
