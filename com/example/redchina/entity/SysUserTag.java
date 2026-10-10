package com.example.redchina.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_tag")
public class SysUserTag extends BaseEntity {
    private Long userId;
    private Long tagId;
}