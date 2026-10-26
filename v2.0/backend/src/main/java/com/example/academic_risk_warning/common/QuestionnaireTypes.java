package com.example.academic_risk_warning.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 问卷类型（v2 第二阶段扩展）。
 *
 * <p>不只评教：还包括学生满意度、心理健康普查、毕业生去向调研、全国本科生学情调查。
 * 每类自带一套题目模板（SCALE = 1~5 量表，TEXT = 选填文本）。</p>
 */
public final class QuestionnaireTypes {

    public static final String TEACHER_STYLE = "TEACHER_STYLE";
    public static final String SATISFACTION = "SATISFACTION";
    public static final String MENTAL_HEALTH = "MENTAL_HEALTH";
    public static final String GRADUATE_SURVEY = "GRADUATE_SURVEY";
    public static final String NATIONAL_SURVEY = "NATIONAL_SURVEY";

    private static final Map<String, String> LABELS = new LinkedHashMap<>();
    private static final Map<String, String> TITLES = new LinkedHashMap<>();

    static {
        LABELS.put(TEACHER_STYLE, "教师风格判定（评教）");
        LABELS.put(SATISFACTION, "学生满意度调查");
        LABELS.put(MENTAL_HEALTH, "心理健康普查");
        LABELS.put(GRADUATE_SURVEY, "毕业生去向调研");
        LABELS.put(NATIONAL_SURVEY, "全国本科生学情调查");

        TITLES.put(TEACHER_STYLE, "教师风格判定问卷");
        TITLES.put(SATISFACTION, "校园学习生活满意度调查");
        TITLES.put(MENTAL_HEALTH, "学生心理健康普查");
        TITLES.put(GRADUATE_SURVEY, "毕业生去向与就业调研");
        TITLES.put(NATIONAL_SURVEY, "全国本科生学情调查（本校问卷）");
    }

    private QuestionnaireTypes() {
    }

    public static List<String> codes() {
        return List.copyOf(LABELS.keySet());
    }

    public static boolean valid(String type) {
        return type != null && LABELS.containsKey(type);
    }

    public static String label(String type) {
        if (type == null) {
            return "问卷";
        }
        return LABELS.getOrDefault(type, type);
    }

    public static String defaultTitle(String type) {
        return TITLES.getOrDefault(type, "问卷");
    }

    /** 发布时用的题目模板：SCALE 为 1~5 量表题，TEXT 为选填文本题 */
    public static String questionJson(String type) {
        switch (type == null ? TEACHER_STYLE : type) {
            case SATISFACTION:
                return "[" + item("Q1", "食堂饭菜质量与价格", "SCALE") + ","
                        + item("Q2", "图书馆座位、借阅与自习环境", "SCALE") + ","
                        + item("Q3", "宿舍环境与生活设施", "SCALE") + ","
                        + item("Q4", "校园安全与后勤服务", "SCALE") + ","
                        + item("Q5", "对学校整体满意度", "SCALE") + ","
                        + item("Q6", "最希望学校改进的一项（选填）", "TEXT") + "]";
            case MENTAL_HEALTH:
                return "[" + item("Q1", "最近两周整体情绪状态", "SCALE") + ","
                        + item("Q2", "最近两周睡眠质量", "SCALE") + ","
                        + item("Q3", "学业压力感受", "SCALE") + ","
                        + item("Q4", "与同学、室友的人际关系", "SCALE") + ","
                        + item("Q5", "其他想说明的情况（选填）", "TEXT") + "]";
            case GRADUATE_SURVEY:
                return "[" + item("Q1", "毕业去向的明确程度", "SCALE") + ","
                        + item("Q2", "目前落实去向的进展", "SCALE") + ","
                        + item("Q3", "对学校就业指导的满意度", "SCALE") + ","
                        + item("Q4", "专业课程对求职的帮助", "SCALE") + ","
                        + item("Q5", "计划去向（升学 / 就业 / 考公 / 创业 / 其他，选填）", "TEXT") + "]";
            case NATIONAL_SURVEY:
                return "[" + item("Q1", "每周课外学习时间投入", "SCALE") + ","
                        + item("Q2", "课程挑战度", "SCALE") + ","
                        + item("Q3", "与教师的互动频率", "SCALE") + ","
                        + item("Q4", "本学期学习收获自评", "SCALE") + ","
                        + item("Q5", "对学校教学的建议（选填）", "TEXT") + "]";
            default:
                return "[" + item("Q1", "该教师讲课节奏是否合适", "SCALE") + ","
                        + item("Q2", "作业量是否合理", "SCALE") + ","
                        + item("Q3", "给分是否严格", "SCALE") + ","
                        + item("Q4", "课堂互动是否充分", "SCALE") + ","
                        + item("Q5", "整体评价", "SCALE") + "]";
        }
    }

    private static String item(String code, String text, String type) {
        return "{\"itemCode\":\"" + code + "\",\"q\":\"" + text + "\",\"type\":\"" + type + "\"}";
    }
}
