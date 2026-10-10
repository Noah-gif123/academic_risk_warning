package com.example.redchina.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_interaction_detail")
public class SysInteractionDetail {
    @TableId(type = IdType.AUTO)
    private Long id;                // 主键ID

    private Long interactionId;     // 关联互动关卡主表ID（sys_interaction.id）

    private String type;            // 互动类型（QUESTION=问答，GAME=小游戏）

    private String content;         // 互动内容（题目/游戏名称）

    private String options;         // 选项（JSON数组，仅问答用）

    private String correctAnswer;   // 正确答案（选项ID/目标状态）

    private String gameRule;        // 游戏规则（仅小游戏用）
}
