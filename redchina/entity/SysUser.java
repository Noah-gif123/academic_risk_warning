package com.example.redchina.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer role;  // USER/ADMIN
    private String userTag;
    private String language;  // zh_CN/en_US/ja_JP/ko_KR
    private Integer status;  // 0-禁用，1-正常

    // 非数据库字段（用于前端展示，不参与ORM映射）
    @TableField(exist = false)
    private String tagNames;  // 用户关联的标签名称（逗号分隔）
}
