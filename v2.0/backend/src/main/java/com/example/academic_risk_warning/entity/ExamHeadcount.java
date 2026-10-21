package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 实际考核人数确认与成绩录入核对表
 * <p>对应表：exam_headcount</p>
 */
@Data
@TableName("exam_headcount")
public class ExamHeadcount {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 开课ID */
    private Long offeringId;

    /** 学期ID */
    private Long termId;

    /** 阶段：MID期中/FINAL期末 */
    private String phase;

    /** 名单人数（选课 ACTIVE 人数） */
    private Integer rosterCount;

    /** 预计考核人数（扣除已审批退课/免修） */
    private Integer expectedCount;

    /** 教务确认的实际考核人数（挂科率分母） */
    private Integer confirmedCount;

    /** 其中缓考人数（不计入分母） */
    private Integer deferredCount;

    /** 其中无成绩待定人数（不计入分母） */
    private Integer noScoreCount;

    /** 与名单人数不一致的原因说明 */
    private String diffReason;

    /** 确认状态：PENDING待确认/CONFIRMED已确认 */
    private String confirmStatus;

    /** 确认人（教务ID） */
    private Long confirmBy;

    /** 确认时间 */
    private LocalDateTime confirmTime;

    /** 实际录入成绩人数 */
    private Integer enteredCount;

    /** 录入核对结果：MATCH一致/MISMATCH不一致/NOT_CHECKED未核对 */
    private String checkResult;

    /** 核对时间 */
    private LocalDateTime checkTime;

    /** 核对说明 */
    private String checkNote;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
