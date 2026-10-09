package com.kmbeast.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * 宠物领养单据信息表，与数据库pet_adopt_order对应
 * */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PetPost {
    private Integer id; //宠物信息表主键ID
    private Integer userId;//用户ID 外键 关联的是用户表
    private Integer petTypeID;//宠物类型ID 外键 关联的是宠物类型表
    private String title;//标题
    private String cover;//封面
    private String content;//内容
    private String summary;//摘要
    private Boolean isAudit;//是否审核（0：未审核；1：已审核）


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;//创建时间

}
