package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 导入批次表（可查可回滚）
 * <p>对应表：import_batch</p>
 */
@Data
@TableName("import_batch")
public class ImportBatch {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 批次号（业务表按此回滚） */
    private String batchNo;

    /** 类型：TIMETABLE周数课次/SCORE成绩/ATTENDANCE考勤/ROSTER名单/PAPER试卷题目/OTHER（已移除 KP） */
    private String importType;

    /** 学期ID（部分导入与学期无关，可空） */
    private Long termId;

    /** 原始文件名 */
    private String fileName;

    /** 总行数 */
    private Integer totalRows;

    /** 成功行数 */
    private Integer successRows;

    /** 失败行数 */
    private Integer failedRows;

    /** 失败明细（行号 + 原因） */
    private String errorJson;

    /** 状态：RUNNING进行中/SUCCESS全部成功/PARTIAL部分成功/FAILED失败/ROLLED_BACK已回滚 */
    private String status;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人角色 */
    private String operatorRole;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 回滚人 */
    private Long rollbackBy;

    /** 回滚时间 */
    private LocalDateTime rollbackTime;

    /** 回滚原因 */
    private String rollbackReason;

    private String remark;

    private LocalDateTime createTime;
}
