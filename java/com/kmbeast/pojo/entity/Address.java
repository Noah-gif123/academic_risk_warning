package com.kmbeast.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * 收货地址信息表，与数据库address对应
 * */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Address {
    private Integer id; //收货地址信息表主键ID
    private Integer userId;//用户ID 外键 关联用户信息表
    private String detail;//详细地址
    private String addressee;//收件人
    private String contentPhone;//收件人联系电话
    private Boolean isDefault;//是否是默认地址（0：非默认；1：默认）
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;//创建时间

}
