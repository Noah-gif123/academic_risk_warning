package com.example.redchina.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_story_node")
public class SysStoryNode extends BaseEntity {
    private Long storyId;
    private String nodeTitle;
    private String nodeContent;
    private String nodeImage;
    private Long parentNodeId;
    private Integer isEnd;  // 0-否，1-是
}
