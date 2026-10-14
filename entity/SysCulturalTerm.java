package com.example.redchina.entity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_cultural_term")
public class SysCulturalTerm extends BaseEntity {
    private String termChinese;
    private String termEnglish;
    private String termJapanese;
    private String termKorean;
    private String termExplain;
    private String termExplainEn;
    private Long tagId;

    @TableField(exist = false)
    private String tagName;
}
