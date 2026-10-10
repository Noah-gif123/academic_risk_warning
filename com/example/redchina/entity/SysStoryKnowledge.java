package com.example.redchina.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_story_knowledge")
public class SysStoryKnowledge extends BaseEntity {
    private Long storyNodeId;
    private String knowledgeTitle;
    private String knowledgeContent;
    private String languageType;
}
