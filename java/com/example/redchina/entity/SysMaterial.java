package com.example.redchina.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文创素材表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_material")
public class SysMaterial extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 素材名称（如「红色祥云纹」「九尾狐图腾」）
    private String materialName;

    // 关联标签ID（sys_tag.id，用于推荐）
    private Long tagId;

    // 素材图片路径
    private String imagePath;

    // 素材描述
    private String description;

    // 语言类型（zh_CN/en_US/ja_JP/ko_KR）
    private String languageType;

    // 下载次数（用于热门排序）
    private Integer downloadCount;
}
