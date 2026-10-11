package com.example.redchina.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_story_option")
public class SysStoryOption extends BaseEntity {
    private Long nodeId;
    private String optionContent;
    private Long nextNodeId;
}
