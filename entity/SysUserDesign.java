package com.example.redchina.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user_design")
public class SysUserDesign {
    @TableId(type = IdType.AUTO)
    private Long id;                // 主键ID

    @TableField("user_id")
    private Long userId;            // 关联用户ID（sys_user.id）

    @TableField("design_name")
    private String designName;      // 设计名称

    @TableField("file_path")
    private String filePath;        // 设计文件存储路径

    @TableField("tags")
    private String tags;            // 标签（逗号分隔）

    @TableField("language_type")
    private String languageType;    // 语言类型（zh_CN/en_US/ja_JP/ko_KR）

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 创建时间
}
