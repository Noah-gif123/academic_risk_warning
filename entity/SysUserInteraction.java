package com.example.redchina.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户-互动关卡结果表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_interaction")
public class SysUserInteraction extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联用户ID（sys_user.id）
    private Long userId;

    // 关联互动关卡ID（sys_interaction.id）
    private Long interactionId;

    // 是否成功（0-失败，1-成功）
    private Integer isSuccess;

    // 得分（仅游戏类互动有效）
    private Integer score;

    // 错误次数（仅问答类互动有效）
    private Integer errorCount;

    // 互动时长（秒）
    private Integer interactionTime;

    // 互动时间（继承BaseEntity的createTime，无需重复定义）
}
