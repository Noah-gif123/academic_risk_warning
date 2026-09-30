package com.example.redchina.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tag")
public class SysTag extends BaseEntity {
    private String tagName;
    private String tagType;  // IP_TYPE/INTANGIBLE_CULTURE/RED_CULTURE
    private String description;
}
