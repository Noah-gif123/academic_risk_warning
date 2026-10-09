package com.kmbeast.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplicationRunListener;

import java.time.LocalDateTime;

/*
 * 宠物领养单据信息表，与数据库pet_adopt_order对应
 * */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PetAdoptOrder {
    private Integer id; //宠物信息表主键ID
    private Integer userId;//用户ID 外键 关联的是用户表
    private Integer petId;//宠物ID 外键 关联的是宠物信息表
    private Integer addressId;//收货地址ID 外键 关联的是收货地址信息表
    private String detail;//领养描述
    private Integer status;//单据状态（1:申请中；2：已审核；3：审核未通过；4：已成功）
    private String auditErrorDetail;//申请不通过原因备注
    private Boolean isAgainPost;//是否再次提交（0：初次提交；1：再次提交）
    private String postNumber;//提交次数

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;//创建时间

}
