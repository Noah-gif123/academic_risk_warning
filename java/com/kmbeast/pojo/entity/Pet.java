package com.kmbeast.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * 宠物信息表，与数据库pet对应
 * */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Pet {
    private Integer id; //宠物信息表主键ID
    private String name;//宠物名
    private String cover;//封面图
    private String detail;//宠物描述
    private String address;//宠物所在地
    private Integer age;//宠物年龄
    private Integer petTypeId;//宠物类别ID 外键 关联宠物类别表
    private Boolean isVaccine;//宠物是否接种疫苗（0：未接种；1：已接种）
    private Boolean isRecommend;//是否推荐（0：不推荐；1：推荐）
    private Boolean isAdopt;//宠物领养状态（0：未领养；1：已领养）
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;//创建时间

}
