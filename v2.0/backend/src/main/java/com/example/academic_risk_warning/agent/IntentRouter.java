package com.example.academic_risk_warning.agent;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * P2-3 IntentRouter：6 类问题的规则路由。
 *
 * <p>规则优先（确定性、可测试），未命中时回退 CLASS_STATS（低置信度）。
 * 后续可在不改调用方的前提下加模型兜底。</p>
 */
@Component
public class IntentRouter {

    public enum Intent {
        CLASS_STATS("班级风险统计"),
        TREND("风险趋势"),
        COURSE_COMPARE("课程/班级对比"),
        STUDENT_RISK("学生个体风险"),
        INTERVENTION("干预与成效"),
        COURSE_QA("课程知识问答");

        private final String label;

        Intent(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record RouteResult(Intent intent, String method, double confidence, String matched) {
    }

    private record Rule(Pattern pattern, Intent intent) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule(Pattern.compile("(干预|复评|成效|T\\+7|有效|改善)"), Intent.INTERVENTION),
            new Rule(Pattern.compile("(知识|定义|原理|复习|习题|资料|课程内容|为什么|怎么理解|检索|知识点|区别|是什么|ACID|事务|特征值|三次握手|栈|队列|极限)"), Intent.COURSE_QA),
            new Rule(Pattern.compile("(学生|学号|这名同学|某个同学|个体)"), Intent.STUDENT_RISK),
            new Rule(Pattern.compile("(趋势|变化|最近.{0,4}周|走势|周报)"), Intent.TREND),
            new Rule(Pattern.compile("(对比|比较|哪个班|跨班|排名|更高)"), Intent.COURSE_COMPARE),
            new Rule(Pattern.compile("(风险|红黄|预警|统计|班级|人数)"), Intent.CLASS_STATS)
    );

    public RouteResult route(String question) {
        if (question == null || question.isBlank()) {
            return new RouteResult(Intent.CLASS_STATS, "FALLBACK_EMPTY", 0.2, "");
        }
        for (Rule rule : RULES) {
            if (rule.pattern().matcher(question).find()) {
                String matched = rule.pattern().matcher(question).results().findFirst()
                        .map(m -> m.group()).orElse("");
                return new RouteResult(rule.intent(), "RULE", 0.95, matched);
            }
        }
        return new RouteResult(Intent.CLASS_STATS, "FALLBACK", 0.3, "");
    }
}
