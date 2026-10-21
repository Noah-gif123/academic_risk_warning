package com.example.academic_risk_warning.util;

import com.example.academic_risk_warning.entity.Question;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 判分口径：
 * ① 客观题（单选 / 多选 / 判断 / 填空）由系统判分；主观题（简答）留给教师。
 * ② 单选、多选、判断、填空均按「全对得分、否则 0 分」，不设部分分。
 * ③ 未作答、空白作答一律 0 分（缺考同样记 0）。
 */
public final class GradeEngine {

    private GradeEngine() {
    }

    public static class Result {
        private final BigDecimal score;
        private final Integer correct;
        private final boolean pending;

        Result(BigDecimal score, Integer correct, boolean pending) {
            this.score = score;
            this.correct = correct;
            this.pending = pending;
        }

        public BigDecimal getScore() {
            return score;
        }

        public Integer getCorrect() {
            return correct;
        }

        public boolean isPending() {
            return pending;
        }
    }

    public static Result grade(Question q, String answerJson) {
        BigDecimal full = q.getFullScore() == null ? BigDecimal.ZERO : q.getFullScore();
        String type = q.getQType() == null ? "" : q.getQType().toUpperCase();
        // 填空与简答一律人工批阅：填空题的写法差异太大（同义词、单位、简写、顺序），
        // 机器逐字比对容易误判，改由教师按空给分，口径更稳、可解释
        if (isManual(type)) {
            return new Result(BigDecimal.ZERO, null, true);
        }
        if (answerJson == null || answerJson.trim().isEmpty()
                || "{}".equals(answerJson.trim()) || "[]".equals(answerJson.trim())) {
            return new Result(BigDecimal.ZERO, 0, false);
        }
        switch (type) {
            case "SINGLE":
            case "MULTI": {
                List<String> std = norm(MiniJson.arrayStrings(q.getAnswerJson(), "keys"));
                List<String> mine = norm(MiniJson.arrayStrings(answerJson, "keys"));
                boolean ok = !std.isEmpty() && std.equals(mine);
                return new Result(ok ? full : BigDecimal.ZERO, ok ? 1 : 0, false);
            }
            case "JUDGE": {
                Boolean std = MiniJson.boolValue(q.getAnswerJson(), "value");
                Boolean mine = MiniJson.boolValue(answerJson, "value");
                boolean ok = std != null && std.equals(mine);
                return new Result(ok ? full : BigDecimal.ZERO, ok ? 1 : 0, false);
            }
            default:
                // 兜底：未识别的题型同样交给老师
                return new Result(BigDecimal.ZERO, null, true);
        }
    }

    /** 需要人工批阅的题型：简答题与填空题 */
    public static boolean isManual(String qType) {
        if (qType == null) {
            return true;
        }
        String t = qType.toUpperCase();
        return "SHORT".equals(t) || "BLANK".equals(t);
    }

    private static List<String> norm(List<String> in) {
        List<String> out = new ArrayList<>();
        for (String s : in) {
            if (s == null) {
                continue;
            }
            String t = s.trim().toUpperCase();
            if (!t.isEmpty() && !out.contains(t)) {
                out.add(t);
            }
        }
        Collections.sort(out);
        return out;
    }
}
