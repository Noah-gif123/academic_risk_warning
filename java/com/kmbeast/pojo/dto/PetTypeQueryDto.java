package com.kmbeast.pojo.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;

/*
 * 宠物类别查询条件类
 * */
@EqualsAndHashCode(callSuper = false)
@Data

public class PetTypeQueryDto extends QueryDto {
    private String name;
}
