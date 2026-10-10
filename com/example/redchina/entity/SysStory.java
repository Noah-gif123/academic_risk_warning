package com.example.redchina.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_story")
public class SysStory extends BaseEntity {
    private String storyTitle;
    private String storyCover;
    private String storyIntro;
    private Long tagId;
    private String tag;
    private Integer isRecommend;  // 0-否，1-是
    private Integer status;  // 0-下架，1-上架

    // 非数据库字段
    @TableField(exist = false)
    private String tagName;  // 标签名称（关联sys_tag）
}
