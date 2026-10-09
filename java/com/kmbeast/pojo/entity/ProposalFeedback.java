package com.kmbeast.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * 建议与反馈信息表，与数据库proposal_feedback对应
 * */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProposalFeedback {
    private Integer id; //建议与反馈信息表主键ID
    private Integer userId;//用户ID 外键 关联的是用户表
    private String detail;//描述
    private Boolean isReply;//是否回复（0：未回复；1：已回复）
    private String replyContent;//回复内容
    private Boolean isTop;//是否是精华帖（0：否；1：是）



    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;//创建时间

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime replyTime;//回复时间

}
