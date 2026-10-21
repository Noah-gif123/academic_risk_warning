package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 运营配置表
 * <p>对应表：system_config</p>
 */
@Data
@TableName("system_config")
public class SystemConfig {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 配置键，如 archive.mid.week */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 值类型：STRING/INT/DECIMAL/BOOL/JSON */
    private String configType;

    /** 分组：ARCHIVE归档/NOTIFY通知/ATTENDANCE考勤/COVERAGE覆盖率/QUESTIONNAIRE问卷/SYSTEM系统 */
    private String configGroup;

    /** 说明 */
    private String description;

    /** 是否允许界面修改 */
    private Integer isEditable;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
