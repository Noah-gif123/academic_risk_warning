package com.example.academic_risk_warning.vo.ai;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 预警解释卡展示对象。
 *
 * <p>一张卡回答三个问题：**为什么是我**（触发原因）、**差多少**（量化差距）、
 * **怎么补**（三条可执行动作，由规则库给出，模型只做表述）。</p>
 */
@Data
public class AlertExplainVO {

    private Long id;
    private Long alertId;
    private Long enrollmentId;
    private String courseName;
    private String className;
    private String studentName;
    private String alertLevel;
    /** ABSENCE / HOMEWORK / PROJECT / SCORE / MULTI / DATA */
    private String triggerType;
    private String triggerTypeText;
    private String summary;
    private List<Section> sections = new ArrayList<>();
    private List<Map<String, Object>> actions = new ArrayList<>();
    private String source;
    private String createTime;

    @Data
    public static class Section {
        private String title;
        private String text;
    }
}
