package com.kmbeast.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * 宠物类别表，与数据库pet_type对应
 * */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PetType {
    private Integer id; //宠物类别表主键ID
    private String name;//宠物类别名



    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;//创建时间

}
