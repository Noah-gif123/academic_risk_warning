package com.kmbeast.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * 行为互动信息表，与数据库active_net对应
 * */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActiveNet {
    private Integer id; //行为互动信息表主键ID
    private Integer userId;//用户ID 外键 关联用户信息表
    private Integer contentId;//内容ID 与内容模块配合使用
    private String contentType;//内容模块
    private Integer type;//行为类型 （1：浏览；2：点赞；3：收藏）
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;//创建时间

}
