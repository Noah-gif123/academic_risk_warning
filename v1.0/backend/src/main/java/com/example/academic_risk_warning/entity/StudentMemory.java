package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生长期记忆（W3）：跨轮次上下文摘要，供后续分析与推荐参考。
 */
@Data
@TableName("student_memory")
public class StudentMemory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    /** 记忆版本（每次刷新 +1） */
    private Integer version;

    /** 压缩后的记忆摘要文本 */
    private String summary;

    /** 产生该记忆的流水线运行ID */
    private Long sourceRunId;

    private LocalDateTime createTime;
}
