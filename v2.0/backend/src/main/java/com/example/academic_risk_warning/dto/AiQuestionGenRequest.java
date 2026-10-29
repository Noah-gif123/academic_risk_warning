package com.example.academic_risk_warning.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 出题请求（M14）。
 *
 * <p>出题范围用 {@link #docIds} 表达：为空表示"本课程全部已入库资料"，否则只用选中的几份
 * （对应教师端「按所选资料出题」的用法）。</p>
 */
@Data
public class AiQuestionGenRequest {

    /** 教学班（决定开课实例与课程） */
    private Long teachingClassId;

    /** 选中的知识库文档 ID（ai_kb_doc.id）；空 = 本课程全部资料 */
    private List<Long> docIds;

    /** 题量，默认 5，上限 10 */
    private Integer count;

    /** 题型：SINGLE / MULTI / JUDGE / BLANK / SHORT；默认 单选 + 判断 */
    private List<String> types;

    /** 难度：easy / medium / hard，默认 medium */
    private String difficulty;

    /** 大题名称（沿用试卷分组名，替代知识点） */
    private String groupName;

    /** 额外要求（如"多考函数参数"），可空 */
    private String instruction;
}
