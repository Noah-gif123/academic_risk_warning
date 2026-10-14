package com.example.redchina.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_interaction")
public class SysInteraction extends BaseEntity {
    private Long storyNodeId; // 关联剧情节点
    private String interactionType; // QUESTION/GAME
    private String content; // 互动内容（JSON格式）
    private Long successNodeId; // 成功后跳转节点
    private Long failNodeId; // 失败后跳转节点
}
