package com.kmbeast.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * 公告信息表，与数据库notice对应
 * */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notice {
    private Integer id; //公告信息表主键ID
    private String title;//标题
    private String content;//内容
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;//创建时间

}
