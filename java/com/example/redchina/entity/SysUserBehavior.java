package com.example.redchina.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user_behavior")
public class SysUserBehavior {
    @TableId(type = IdType.AUTO)
    private Long id;                // 主键ID

    @TableField("user_id")
    private Long userId;            // 关联用户ID（sys_user.id）

    @TableField("behavior_type")
    private String behaviorType;    // 行为类型（BROWSE/COMPLETE/INTERACT/UNLOCK）

    @TableField("target_id")
    private Long targetId;          // 目标ID（剧情/互动/知识点ID）

    @TableField("target_type")
    private String targetType;      // 目标类型（STORY/INTERACTION/KNOWLEDGE）

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 行为时间
}
