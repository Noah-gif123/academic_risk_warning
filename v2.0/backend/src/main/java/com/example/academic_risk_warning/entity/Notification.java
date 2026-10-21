package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 通知主体表（聚合与业务关联）
 * <p>对应表：notification</p>
 */
@Data
@TableName("notification")
public class Notification {

    /** 通知ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 业务类型：ALERT_NEW/ALERT_UPGRADE/CLASS_ALERT班集体提醒/ARCHIVE_TASK归档待办/ARCHIVE_RESULT归档结果/COVERAGE_REMIND覆盖率提醒/SCORE_TASK打分待办/MULTI_COURSE多科目预警/REFER_RESULT上报结果/QUESTIONNAIRE问卷/SYSTEM系统 */
    private String bizType;

    /** 关联业务ID（预警/归档批次/开课/问卷等） */
    private Long bizId;

    /** 标题 */
    private String title;

    /** 正文 */
    private String content;

    /** 结构化载荷（跳转参数、明细列表等） */
    private String payloadJson;

    /** 聚合键：如 CLASS_ALERT:{counselorId}:{termId}:{weekNo}，同键只发一条汇总 */
    private String aggregateKey;

    /** 本通知聚合了多少条明细（防骚扰：一次一条汇总） */
    private Integer aggregateCount;

    /** 接收范围描述，如"软件1–4班导员 + 教务处" */
    private String audienceDesc;

    /** 优先级：HIGH/NORMAL/LOW */
    private String priority;

    /** 过期时间 */
    private LocalDateTime expireTime;

    private Long createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
