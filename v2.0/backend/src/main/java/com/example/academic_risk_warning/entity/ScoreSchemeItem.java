package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 评分项定义表（两级；考勤不得入内）
 * <p>对应表：score_scheme_item</p>
 */
@Data
@TableName("score_scheme_item")
public class ScoreSchemeItem {

    /** 评分项ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 构成ID */
    private Long schemeId;

    /** 父项ID（NULL=一级项；非空=二级项） */
    private Long parentId;

    /** 层级：1一级项 2二级项 */
    private Integer itemLevel;

    /** 评分项编码（同一构成内唯一），如 USUAL / HW / FINAL_EXAM */
    private String itemCode;

    /** 评分项名称，如 课后作业 / 三级项目答辩 */
    private String itemName;

    /** 大类（仅用于统计与展示，不限制项的组合）：HOMEWORK作业/QUIZ课堂测试/PERFORMANCE课堂表现/EXPERIMENT实验/PROJECT项目/PRACTICE实践/MIDTERM期中/FINAL期末/OTHER其他 */
    private String category;

    /** 权重(%)：一级项占最终总评、二级项占其父项；同级合计必须=100 */
    private BigDecimal weight;

    /** 该项满分 */
    private BigDecimal fullScore;

    /** 数据来源：AUTO系统自动算（作业/机考）/MANUAL老师录入 */
    private String dataSource;

    /** 分数形态：NUMERIC数值/PASS_FAIL通过制/GRADE等级制 */
    private String scoreType;

    /** 分数映射（等级制/通过制→数值），如 {"优秀":95,"良好":85,"通过":85,"不通过":50} */
    private String mappingJson;

    /** 预计次数（如作业 9 次、实验 5 次） */
    private Integer expectedTimes;

    /** 多次项汇总方式：AVG均值/SUM累加/MAX最高/LAST最近一次 */
    private String aggregateMode;

    /** 是否锚点项（终结性收口项）：1是（统一命题闭卷/机考/大作业答辩），供通道 C 校准用 */
    private Integer isAnchor;

    /** 锚点层级：A统一命题考核/NONE无（"大一摸底考""高考分"两条判定逻辑已废弃） */
    private String anchorLayer;

    /** 是否主观项：1是（分项级 Δ 只在主观项上估计） */
    private Integer isSubjective;

    /** 打分主体：TEACHER/ADMIN/SYSTEM */
    private String scorerRole;

    /** 排序号 */
    private Integer sortNo;

    private String status;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
