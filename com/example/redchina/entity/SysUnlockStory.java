package com.example.redchina.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_unlock_story")
public class SysUnlockStory extends BaseEntity {
    private Long userId;
    private Long storyId;
    private Long unlockNodeId;
    private Integer isComplete;  // 0-否，1-是
    private LocalDateTime unlockTime;
}
