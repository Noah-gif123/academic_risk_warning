package com.example.academic_risk_warning.service.ai;

import java.util.List;
import java.util.Map;

/**
 * 预警触发类型一致性闸门（Q1 的第三道校验）。
 *
 * <p>同一张预警两次生成，模型可能一次说"分数临界"、一次说"缺勤超限"——数字都对、结构也对，
 * 但结论指向的规则不是同一条。这个闸门按触发类型的关键词做交叉检查：</p>
 *
 * <ul>
 *   <li>正文命中了本类型关键词 → 通过（不苛求措辞完全一致）；</li>
 *   <li>没命中本类型、却命中了别的类型 → 判为串了口径，交给调用方重写；</li>
 *   <li>两者都没命中（纯定性描述） → 放行，避免把正常表述误伤。</li>
 * </ul>
 *
 * <p>独立成类是为了能单测：线上走 Dify/直连两条通道，靠自然运行很难稳定复现"串口径"。</p>
 */
public final class TriggerGate {

    /** 触发类型 → 该类型在文案里必然出现的说法 */
    private static final Map<String, List<String>> WORDS = Map.of(
            "ABSENCE", List.of("缺勤", "旷课", "考勤", "出勤", "迟到", "请假"),
            "HOMEWORK", List.of("作业", "缺交", "未交", "补交"),
            "PROJECT", List.of("项目", "大作业", "实验"),
            "SCORE", List.of("分数", "总分", "成绩", "临界", "排名"),
            "EXAM", List.of("期末", "考试", "卷面"));

    private static final Map<String, String> TYPE_TEXT = Map.of(
            "ABSENCE", "缺勤超限",
            "HOMEWORK", "作业欠交",
            "PROJECT", "项目掉队",
            "SCORE", "分数临界",
            "EXAM", "期末考试",
            "MULTI", "多门联动",
            "DATA", "数据不足");

    private TriggerGate() {
    }

    public static String typeText(String triggerType) {
        return TYPE_TEXT.getOrDefault(triggerType == null ? "" : triggerType, "综合风险");
    }

    /**
     * @return {@code null} 表示一致；否则返回一句可直接展示/回灌给模型的问题描述
     */
    public static String mismatch(String text, String triggerType) {
        if (text == null || text.isBlank()) {
            return "输出为空";
        }
        List<String> expect = WORDS.get(triggerType);
        if (expect == null) {
            return null;   // MULTI / DATA 不参与一致性校验
        }
        if (expect.stream().anyMatch(text::contains)) {
            return null;
        }
        for (Map.Entry<String, List<String>> e : WORDS.entrySet()) {
            if (e.getKey().equals(triggerType)) {
                continue;
            }
            if (e.getValue().stream().anyMatch(text::contains)) {
                return "预警触发类型是「" + typeText(triggerType) + "」，但正文通篇在讲「" + typeText(e.getKey()) + "」";
            }
        }
        return null;
    }
}
