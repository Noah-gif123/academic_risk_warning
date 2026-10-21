package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.util.List;

/** 消息中心分页结果 */
@Data
public class NotificationPageVO {
    /** 当前页消息 */
    private List<NotificationVO> items;
    /** 满足条件的总条数（未读数 / 全部数） */
    private long total;
    /** 当前人的未读总数（不随筛选变化） */
    private long unread;
    /** 页码，从 1 开始 */
    private int page;
    /** 每页条数 */
    private int size;
}
