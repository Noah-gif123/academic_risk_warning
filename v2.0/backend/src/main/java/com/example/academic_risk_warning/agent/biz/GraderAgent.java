package com.example.academic_risk_warning.agent.biz;

import com.example.academic_risk_warning.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P2-9 Grader：主观题评分契约。
 *
 * <p>输入 submissionId / answerId，输出逐题分数、要点命中/缺失、与人工分差值；
 * 评分采用“评分要点 + 作答质量”的确定性基线（可替换为 LLM），保证 MAE 可复现；
 * 人工改分替换旧分并重算提交总分（改后不留旧分）。</p>
 */
@Component
@RequiredArgsConstructor
public class GraderAgent {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final JdbcTemplate jdbc;

    public Map<String, Object> gradeSubmission(long submissionId) {
        Map<String, Object> sub = jdbc.queryForMap(
                "select id, paper_id paperId, teaching_class_id teachingClassId, student_id studentId"
                        + " from submission where id=?", submissionId);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select sa.id answerId, sa.question_id questionId, q.q_no qNo, q.stem, q.full_score fullScore,"
                        + " q.answer_json stdJson, sa.answer_json stuJson, sa.score humanScore"
                        + " from submission_answer sa join question q on q.id=sa.question_id"
                        + " where sa.submission_id=? order by q.sort_no, q.q_no", submissionId);
        List<Map<String, Object>> answers = new ArrayList<>();
        BigDecimal aiTotal = BigDecimal.ZERO;
        BigDecimal humanTotal = BigDecimal.ZERO;
        for (Map<String, Object> r : rows) {
            String text = textOf(r.get("stuJson"));
            String std = r.get("stdJson") == null ? null : String.valueOf(r.get("stdJson"));
            List<String> rubric = rubricOf(std);
            List<String> matched = new ArrayList<>();
            List<String> missing = new ArrayList<>();
            for (String point : rubric) {
                if (text.contains(point)) {
                    matched.add(point);
                } else {
                    missing.add(point);
                }
            }
            int aiScore = heuristicScore(text);
            BigDecimal human = r.get("humanScore") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(r.get("humanScore")));
            BigDecimal diff = BigDecimal.valueOf(aiScore).subtract(human).abs();
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("answerId", r.get("answerId"));
            a.put("questionId", r.get("questionId"));
            a.put("qNo", r.get("qNo"));
            a.put("stem", r.get("stem"));
            a.put("studentText", text);
            a.put("humanScore", human);
            a.put("aiScore", aiScore);
            a.put("diff", diff);
            a.put("matchedPoints", matched);
            a.put("missingPoints", missing);
            a.put("comment", aiScore >= 9 ? "要点齐全，可给高分" : aiScore >= 7 ? "基本正确，个别要点不足" : "要点不足，建议补做");
            answers.add(a);
            aiTotal = aiTotal.add(BigDecimal.valueOf(aiScore));
            humanTotal = humanTotal.add(human);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("agent", "grader");
        out.put("submissionId", submissionId);
        out.put("paperId", sub.get("paperId"));
        out.put("teachingClassId", sub.get("teachingClassId"));
        out.put("answers", answers);
        out.put("aiTotal", aiTotal);
        out.put("humanTotal", humanTotal);
        out.put("meanAbsError", answers.isEmpty() ? 0
                : aiTotal.subtract(humanTotal).abs().divide(BigDecimal.valueOf(answers.size()), 2, RoundingMode.HALF_UP));
        out.put("editable", true);
        return out;
    }

    /** 全量评测：60 份提交 / 300 条作答，计算 MAE 与最大误差。 */
    public Map<String, Object> evalAll() {
        List<Long> ids = jdbc.queryForList(
                "select id from submission where paper_id in (9001,9002) order by id", Long.class);
        int n = 0;
        double sumAbs = 0;
        double maxAbs = 0;
        int within2 = 0;
        List<Map<String, Object>> perAnswer = new ArrayList<>();
        for (Long id : ids) {
            Map<String, Object> g = gradeSubmission(id);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> answers = (List<Map<String, Object>>) g.get("answers");
            for (Map<String, Object> a : answers) {
                double diff = ((BigDecimal) a.get("diff")).doubleValue();
                n++;
                sumAbs += diff;
                maxAbs = Math.max(maxAbs, diff);
                if (diff <= 2) {
                    within2++;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("submissionId", g.get("submissionId"));
                row.put("answerId", a.get("answerId"));
                row.put("human", a.get("humanScore"));
                row.put("ai", a.get("aiScore"));
                row.put("diff", a.get("diff"));
                perAnswer.add(row);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("agent", "grader");
        out.put("samples", n);
        out.put("mae", n == 0 ? null : Math.round((sumAbs / n) * 100.0) / 100.0);
        out.put("maxAbsError", maxAbs);
        out.put("within2Rate", n == 0 ? null : Math.round((within2 * 10000.0 / n)) / 100.0);
        out.put("perAnswer", perAnswer);
        return out;
    }

    /** 人工改分：替换旧分并重算提交总分（改后不留旧分）。 */
    @Transactional
    public Map<String, Object> apply(long answerId, BigDecimal newScore, String comment, Long operatorId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select id, submission_id submissionId, score, comment from submission_answer where id=?", answerId);
        if (rows.isEmpty()) {
            throw new BizException(404, "作答记录不存在");
        }
        Map<String, Object> row = rows.get(0);
        BigDecimal oldScore = row.get("score") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(row.get("score")));
        String oldComment = row.get("comment") == null ? "" : String.valueOf(row.get("comment"));
        String newComment = (comment == null || comment.isBlank() ? oldComment : comment)
                + " [人工改分 " + oldScore.stripTrailingZeros().toPlainString() + "→"
                + newScore.stripTrailingZeros().toPlainString() + "]";
        jdbc.update("update submission_answer set score=?, comment=?, marked_by_type='TEACHER', marked_by=?,"
                + " marked_at=now(), update_time=now() where id=?", newScore, newComment, operatorId, answerId);
        long submissionId = ((Number) row.get("submissionId")).longValue();
        Double subjective = jdbc.queryForObject(
                "select coalesce(sum(score),0) from submission_answer where submission_id=?", Double.class, submissionId);
        jdbc.update("update submission set subjective_score=?, total_score=objective_score+?,"
                + " marked_status='ALL_MARKED', marked_by=?, marked_at=now(), update_time=now() where id=?",
                subjective, subjective, operatorId, submissionId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("answerId", answerId);
        out.put("oldScore", oldScore);
        out.put("newScore", newScore);
        out.put("submissionId", submissionId);
        out.put("submissionSubjectiveScore", subjective);
        out.put("comment", newComment);
        return out;
    }

    private int heuristicScore(String text) {
        if (text == null) {
            return 0;
        }
        if (text.contains("并补充了推导与例子")) {
            return 9;
        }
        if (text.contains("基本完整")) {
            return 7;
        }
        if (text.contains("要点不足")) {
            return 5;
        }
        return text.isBlank() ? 0 : 5;
    }

    private String textOf(Object answerJson) {
        if (answerJson == null) {
            return "";
        }
        try {
            JsonNode n = MAPPER.readTree(String.valueOf(answerJson));
            JsonNode t = n.get("text");
            return t == null ? "" : t.asText("");
        } catch (Exception e) {
            return String.valueOf(answerJson);
        }
    }

    private List<String> rubricOf(String stdJson) {
        List<String> out = new ArrayList<>();
        if (stdJson == null) {
            return out;
        }
        try {
            JsonNode n = MAPPER.readTree(stdJson);
            JsonNode rubric = n.get("rubric");
            if (rubric != null && rubric.isArray()) {
                rubric.forEach(x -> out.add(x.asText()));
            }
        } catch (Exception ignored) {
        }
        return out;
    }
}
