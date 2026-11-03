package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.vo.ai.AdminClassFact;
import com.example.academic_risk_warning.vo.ai.ClassFact;
import com.example.academic_risk_warning.vo.ai.OfferingFact;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 报告数字对账（可信性评估用）。
 *
 * <p>现有的「越界数字闸门」只能证明"正文里的数字都在事实包白名单范围内"，
 * 白名单本身很宽（0~10、整十数、年份都放行），所以「平均 81 分」写成「平均 80 分」是拦不住的。
 * 这里换一个方向做**定向抽取**：只盯住结论里最关键的几类数字（红/黄/绿人数、平均/最低/最高分、
 * 及格线、风险率、课程数、教学班数、未关闭预警数），把正文里的说法和事实包逐项比对，
 * 输出"对账明细"给用户看，也便于论文里统计「对账通过率」。</p>
 *
 * <p>容差：人数/条数/门数要求完全相等；分数与百分比允许 0.6 的四舍五入误差
 * （81.11 写成 81 算一致，写成 80 就算不一致）。</p>
 */
public final class NumberAuditor {

    private NumberAuditor() {
    }

    /**
     * 一份报告里的关键数字（三类事实包统一摊平，避免审计逻辑分三套）。
     *
     * <p>{@code perClass}：课程级报告里会写"02班红 2 人"这类**分班数字**，
     * 它们不等于课程总数，所以要一起带上——只要与某个教学班的值对上，就算一致。</p>
     */
    public record Facts(Integer studentCount, Integer red, Integer yellow, Integer green,
                        Integer courseCount, Integer classCount, Integer enrollmentCount,
                        BigDecimal avgScore, BigDecimal minScore, BigDecimal maxScore,
                        Integer passScore, BigDecimal failRate,
                        Integer absentOver, Integer activeAlerts,
                        Map<String, List<Alt>> perClass) {

        public Facts(Integer studentCount, Integer red, Integer yellow, Integer green,
                     Integer courseCount, Integer classCount, Integer enrollmentCount,
                     BigDecimal avgScore, BigDecimal minScore, BigDecimal maxScore,
                     Integer passScore, BigDecimal failRate,
                     Integer absentOver, Integer activeAlerts) {
            this(studentCount, red, yellow, green, courseCount, classCount, enrollmentCount,
                    avgScore, minScore, maxScore, passScore, failRate, absentOver, activeAlerts, null);
        }
    }

    /** 分班数值：value 是数字，label 用来在明细里说明"这是哪个班的" */
    public record Alt(String value, String label) {
    }

    /** 一项对账结果 */
    public record Check(String label, String claim, String expected, boolean ok) {
    }

    public static Facts fromClass(ClassFact f) {
        return new Facts(f.getStudentCount(), f.getRedCount(), f.getYellowCount(), f.getGreenCount(),
                null, 1, f.getStudentCount(), f.getAvgScore(), f.getMinScore(), f.getMaxScore(),
                f.getPassScore(), null, f.getAbsentOverLimitCount(), f.getActiveAlertCount());
    }

    public static Facts fromAdminClass(AdminClassFact f) {
        return new Facts(f.getStudentCount(), f.getRedCount(), f.getYellowCount(), f.getGreenCount(),
                f.getCourseCount(), null, f.getEnrollmentCount(), f.getAvgScore(), null, null,
                null, f.getFailRate(), null, f.getActiveAlertCount());
    }

    public static Facts fromOffering(OfferingFact f) {
        Map<String, List<Alt>> perClass = new LinkedHashMap<>();
        for (OfferingFact.ClassRow c : f.getClasses()) {
            String label = c.getClassName();
            addAlt(perClass, "红灯人数", c.getRed(), label);
            addAlt(perClass, "黄灯人数", c.getYellow(), label);
            addAlt(perClass, "绿灯人数", c.getGreen(), label);
            addAlt(perClass, "学生人数", c.getStudentCount(), label);
            addAlt(perClass, "平均分", c.getAvgScore(), label);
            addAlt(perClass, "最低分", c.getMinScore(), label);
            addAlt(perClass, "最高分", c.getMaxScore(), label);
        }
        return new Facts(f.getStudentCount(), f.getRedCount(), f.getYellowCount(), f.getGreenCount(),
                null, f.getClassCount(), f.getStudentCount(), f.getAvgScore(), f.getMinScore(), f.getMaxScore(),
                f.getPassScore(), f.getFailRate(), f.getAbsentOverLimitCount(), f.getActiveAlertCount(), perClass);
    }

    private static void addAlt(Map<String, List<Alt>> acc, String label, Object value, String classLabel) {
        if (value == null) {
            return;
        }
        acc.computeIfAbsent(label, k -> new ArrayList<>()).add(new Alt(str(value), classLabel));
    }

    /** 一条抽取规则：标签 + 正则（第 1 组是数字）+ 取期望值的函数 */
    private record Rule(String label, Pattern pattern, java.util.function.Function<Facts, Object> expect, boolean decimal) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("红灯人数", Pattern.compile("红(?:灯)?\\s*(\\d{1,3})\\s*人"), Facts::red, false),
            new Rule("黄灯人数", Pattern.compile("黄(?:灯)?\\s*(\\d{1,3})\\s*人"), Facts::yellow, false),
            new Rule("绿灯人数", Pattern.compile("绿(?:灯)?\\s*(\\d{1,3})\\s*人"), Facts::green, false),
            new Rule("红黄灯合计", Pattern.compile("红黄灯(?:合计|共)?\\s*(\\d{1,3})\\s*人"),
                    f -> f.red() == null || f.yellow() == null ? null : f.red() + f.yellow(), false),
            // 「合计 N 人」不一定是总人数（也可能是"红黄灯合计 11 人"），所以排除掉那种写法
            new Rule("学生人数", Pattern.compile("(?<!红黄灯)(?:合计|共|现有学生|全班|本班)\\s*(\\d{1,3})\\s*人"),
                    Facts::studentCount, false),
            new Rule("平均分", Pattern.compile("平均(?:总分|分)?[^0-9%]{0,6}(\\d{1,3}(?:\\.\\d+)?)\\s*分"), Facts::avgScore, true),
            new Rule("最低分", Pattern.compile("最低[^0-9%]{0,4}(\\d{1,3}(?:\\.\\d+)?)\\s*分"), Facts::minScore, true),
            new Rule("最高分", Pattern.compile("最高[^0-9%]{0,4}(\\d{1,3}(?:\\.\\d+)?)\\s*分"), Facts::maxScore, true),
            new Rule("及格线", Pattern.compile("及格线[^0-9%]{0,4}(\\d{1,3})"), Facts::passScore, false),
            new Rule("风险率", Pattern.compile("(?:挂科风险率|红灯占比|风险率)[^0-9%]{0,4}(\\d{1,3}(?:\\.\\d+)?)\\s*%"), Facts::failRate, true),
            new Rule("课程门数", Pattern.compile("(\\d{1,3})\\s*门(?:课程|课)"), Facts::courseCount, false),
            new Rule("教学班数", Pattern.compile("(\\d{1,3})\\s*个教学班"), Facts::classCount, false),
            new Rule("选课记录", Pattern.compile("选课记录[^0-9]{0,4}(\\d{1,3})\\s*条"), Facts::enrollmentCount, false),
            new Rule("缺勤超上限", Pattern.compile("缺勤超上限[^0-9]{0,4}(\\d{1,3})\\s*人"), Facts::absentOver, false),
            new Rule("未关闭预警", Pattern.compile("(?:未关闭预警事件|预警事件)[^0-9]{0,6}(\\d{1,3})\\s*条"), Facts::activeAlerts, false)
    );

    /**
     * 逐条规则抽取并比对。
     *
     * <p>同一字段可能出现多次（"红灯 1 人"…"另有红灯 4 人需要约谈"），
     * 所以按**不同说法**各记一条，最多 3 条：只说第一个会漏掉后半段的矛盾说法。</p>
     */
    public static List<Check> audit(String text, Facts facts) {
        List<Check> out = new ArrayList<>();
        if (text == null || text.isBlank() || facts == null) {
            return out;
        }
        String flat = text.replace('\n', ' ').replaceAll("\\s{2,}", " ");
        for (Rule rule : RULES) {
            Object expected = rule.expect().apply(facts);
            if (expected == null) {
                continue;
            }
            Matcher m = rule.pattern().matcher(flat);
            Set<String> seen = new LinkedHashSet<>();
            while (m.find() && seen.size() < 3) {
                String claim = m.group(1);
                if (!seen.add(claim)) {
                    continue;
                }
                String expectedText = str(expected);
                boolean ok = same(claim, expected, rule.decimal());
                if (!ok && facts.perClass() != null) {
                    // 课程级报告里的"02班红 2 人"这类分班说法：与某个教学班的值对上也算一致
                    for (Alt alt : facts.perClass().getOrDefault(rule.label(), List.of())) {
                        if (same(claim, alt.value(), rule.decimal())) {
                            ok = true;
                            expectedText = alt.value() + "（" + alt.label() + "）";
                            break;
                        }
                    }
                }
                out.add(new Check(rule.label(), claim, expectedText, ok));
            }
        }
        return out;
    }

    /** 正文段落拼成一段纯文本（对账用） */
    public static String flatten(List<String> texts) {
        StringBuilder sb = new StringBuilder();
        for (String t : texts) {
            if (t != null && !t.isBlank()) {
                sb.append(t).append('\n');
            }
        }
        return sb.toString();
    }

    /** 写进 content_json 的对账块 */
    public static Map<String, Object> toJsonBlock(List<Check> checks) {
        Map<String, Object> block = new LinkedHashMap<>();
        int passed = 0;
        List<Map<String, Object>> items = new ArrayList<>();
        for (Check c : checks) {
            if (c.ok()) {
                passed++;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("label", c.label());
            item.put("claim", c.claim());
            item.put("expected", c.expected());
            item.put("ok", c.ok());
            items.add(item);
        }
        block.put("total", checks.size());
        block.put("passed", passed);
        block.put("items", items);
        return block;
    }

    /** 不一致项的可读摘要（写进 ai_run_step 的 detail） */
    public static String mismatchSummary(List<Check> checks) {
        Set<String> parts = new LinkedHashSet<>();
        for (Check c : checks) {
            if (!c.ok()) {
                parts.add(c.label() + "：报告写 " + c.claim() + "，事实为 " + c.expected());
            }
        }
        return String.join("；", parts);
    }

    private static boolean same(String claim, Object expected, boolean decimal) {
        BigDecimal c;
        BigDecimal e;
        try {
            c = new BigDecimal(claim);
            e = new BigDecimal(str(expected));
        } catch (Exception ex) {
            return false;
        }
        BigDecimal diff = c.subtract(e).abs();
        if (!decimal) {
            return diff.compareTo(BigDecimal.valueOf(0.5)) < 0;
        }
        return diff.compareTo(BigDecimal.valueOf(0.6)) <= 0;
    }

    private static String str(Object v) {
        if (v == null) {
            return "-";
        }
        if (v instanceof BigDecimal b) {
            return b.setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(v);
    }
}
