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
        if (!"SHORT".equals(type) && (answerJson == null || answerJson.trim().isEmpty()
                || "{}".equals(answerJson.trim()) || "[]".equals(answerJson.trim()))) {
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
            case "BLANK": {
                List<String> std = MiniJson.arrayStrings(q.getAnswerJson(), "answers");
                List<String> mine = MiniJson.arrayStrings(answerJson, "answers");
                if (std.isEmpty()) {
                    return new Result(BigDecimal.ZERO, 0, false);
                }
                boolean ignoreCase = !Boolean.FALSE.equals(MiniJson.boolValue(q.getAnswerJson(), "ignoreCase"));
                boolean trim = !Boolean.FALSE.equals(MiniJson.boolValue(q.getAnswerJson(), "trim"));
                boolean ok = std.size() == mine.size();
                if (ok) {
                    for (int i = 0; i < std.size(); i++) {
                        String a = std.get(i);
                        String b = mine.get(i) == null ? "" : mine.get(i);
                        if (trim) {
                            a = a.trim();
                            b = b.trim();
                        }
                        if (!(ignoreCase ? a.equalsIgnoreCase(b) : a.equals(b))) {
                            ok = false;
                            break;
                        }
                    }
                }
                return new Result(ok ? full : BigDecimal.ZERO, ok ? 1 : 0, false);
            }
            default:
                // 简答题：等教师批阅
                return new Result(BigDecimal.ZERO, null, true);
        }
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
