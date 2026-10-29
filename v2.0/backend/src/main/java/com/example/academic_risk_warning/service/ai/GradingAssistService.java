package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.config.AiProperties;
import com.example.academic_risk_warning.entity.Paper;
import com.example.academic_risk_warning.service.PaperService;
import com.example.academic_risk_warning.util.GradeEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 批改辅助（M15）：给「填空 / 简答」这类主观题出<b>建议分 + 评语</b>，教师确认后才生效。
 *
 * <p>三条硬约束，和出题（M14）保持同一套价值观：</p>
 * <ol>
 *   <li><b>AI 不写分数</b>：建议存在 {@code ai_grading_suggest}，教师点「采纳」后仍走原来的
 *       {@code POST /api/teacher/submissions/{id}/grade} 写分，判卷人与审计链路不变；</li>
 *   <li><b>分数必须可解释</b>：提示词要求按「参考要点逐条给分」，模型给的 rubic 要点一并存下来；</li>
 *   <li><b>三道校验</b>：分数落在 [0, 满分] 内、评语里的数字只能用满分/得分或参考要点里出现过的、
 *       参考要点为空时只给分档建议并标注，避免"看起来很专业其实没依据"。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradingAssistService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PROMPT_VERSION = "grading-assist-v1";
    /** 单次最多批几道主观题（一张卷子通常 2–5 道） */
    private static final int MAX_ITEMS = 8;

    private final JdbcTemplate jdbc;
    private final PaperService paperService;
    private final DashScopeClient client;
    private final AiRunService runService;
    private final AiProperties props;

    // ============================================================ 生成建议
    @Transactional
    public Map<String, Object> generate(Long submissionId, Long teacherId) {
        if (!props.isEnabled()) {
            throw new BizException("AI 未启用（ai.enabled=false），无法生成批改建议");
        }
        Map<String, Object> sub = one("select id, paper_id, teaching_class_id, student_id from submission where id = ?",
                submissionId);
        if (sub == null) {
            throw new BizException("提交记录不存在");
        }
        Paper paper = paperService.requireMyPaper(lng(sub.get("paper_id")), teacherId);

        List<Map<String, Object>> items = jdbc.queryForList("""
                select sa.id as answer_id, sa.submission_id as submission_id, q.id as question_id, q.q_no,
                       q.q_type, q.stem, q.answer_json as std_json, q.analysis, q.full_score,
                       sa.answer_json as stu_json
                from submission_answer sa
                         join question q on q.id = sa.question_id
                where sa.submission_id = ?
                order by q.sort_no, q.q_no
                """, submissionId);
        List<Map<String, Object>> manual = new ArrayList<>();
        for (Map<String, Object> r : items) {
            if (GradeEngine.isManual(String.valueOf(r.get("q_type")))) {
                Map<String, Object> m = new LinkedHashMap<>(r);
                m.put("teaching_class_id", sub.get("teaching_class_id"));
                manual.add(m);
            }
        }
        if (manual.isEmpty()) {
            throw new BizException("这张卷子没有主观题（填空 / 简答），AI 批改用不上");
        }
        if (manual.size() > MAX_ITEMS) {
            manual = manual.subList(0, MAX_ITEMS);
        }
        if (runService.overLimit(teacherId, "GRADING")) {
            throw new BizException("今日 AI 批改次数已达上限（" + props.getDailyLimit() + " 次），请明天再试");
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("submissionId", submissionId);
        out.put("paperId", paper.getId());
        out.put("paperName", paper.getPaperName());
        out.put("teachingClassId", sub.get("teaching_class_id"));

        // 空作答不走模型：直接 0 分 + "未作答"，省 token 也避免模型给同情分
        List<Map<String, Object>> pending = new ArrayList<>();
        List<Map<String, Object>> suggestions = new ArrayList<>();
        for (Map<String, Object> r : manual) {
            String stuText = plainAnswer(r.get("stu_json"));
            if (stuText.isBlank()) {
                suggestions.add(saveSuggestion(r, BigDecimal.ZERO, "未作答，记 0 分。",
                        List.of(), gateAll(true, true, true, "空作答按规则处理"), "TEMPLATE", null, 0, null));
            } else {
                pending.add(r);
            }
        }

        int tokens = 0;
        Long runId = null;
        List<Map<String, Object>> skipped = new ArrayList<>();
        if (!pending.isEmpty()) {
            if (!props.directReady()) {
                throw new BizException("模型通道未配置（app.ai.llm-* 或 dashscope-* 为空），无法生成批改建议");
            }
            runId = runService.start("GRADING", "SUBMISSION", submissionId, teacherId);
            long t0 = System.currentTimeMillis();
            String userPrompt = buildPrompt(paper, pending);
            runService.step(runId, 1, "load-answers", "submission=" + submissionId,
                    "主观题 " + pending.size() + " 道", "PASS", null, System.currentTimeMillis() - t0);

            DashScopeClient.ChatResult res = client.chat(SYSTEM_PROMPT, userPrompt, 0.2, 1400);
            tokens = res.getTotalTokens();
            if (!res.isOk()) {
                runService.step(runId, 2, "llm-grade", "grading", null, "FAIL", res.getError(),
                        System.currentTimeMillis() - t0);
                runService.finish(runId, "FAILED", "LLM", tokens, res.getError());
                runService.usage(runId, "GRADING", tokens, teacherId);
                throw new BizException("生成批改建议失败：" + res.getError());
            }
            List<Map<String, Object>> parsed = parse(res.getText());
            runService.step(runId, 2, "llm-grade", "grading", "解析 " + parsed.size() + " 条建议", "PASS", null,
                    System.currentTimeMillis() - t0);

            int pass = 0;
            for (int i = 0; i < pending.size(); i++) {
                Map<String, Object> r = pending.get(i);
                Map<String, Object> p = i < parsed.size() ? parsed.get(i) : null;
                if (p == null) {
                    skipped.add(Map.of("answerId", r.get("answer_id"), "reason", "模型没有返回这一题"));
                    continue;
                }
                BigDecimal full = dec(r.get("full_score"));
                BigDecimal score = p.get("score") == null ? null : dec(p.get("score"));
                List<String> problems = new ArrayList<>();
                if (score == null) {
                    skipped.add(Map.of("answerId", r.get("answer_id"), "reason", "模型没给出可解析的分数"));
                    continue;
                }
                if (score.signum() < 0 || score.compareTo(full) > 0) {
                    score = score.signum() < 0 ? BigDecimal.ZERO : full;
                    problems.add("分数超出 [0," + full + "] 已截断");
                }
                String comment = str(p.get("comment"));
                if (comment.isBlank()) {
                    comment = "（AI 未给出评语）";
                }
                List<String> badNumbers = badNumbers(comment, full, str(r.get("std_json")), str(r.get("stem")));
                boolean refOk = !str(r.get("std_json")).isBlank() && !plainRef(r.get("std_json")).isBlank();
                List<Map<String, Object>> checks = gateAll(problems.isEmpty(), badNumbers.isEmpty(), refOk,
                        problems.isEmpty() && badNumbers.isEmpty()
                                ? (refOk ? "ok" : "本题没有参考要点，仅给分档建议")
                                : joinProblems(problems, badNumbers));
                if (problems.isEmpty() && badNumbers.isEmpty()) {
                    pass++;
                }
                suggestions.add(saveSuggestion(r, score, comment, List.of(), checks, "LLM", runId, tokens, p));
            }
            runService.step(runId, 3, "gates", "score-range / comment-digits / ref-exists",
                    "通过 " + pass + "/" + pending.size(), pass > 0 ? "PASS" : "FAIL", null,
                    System.currentTimeMillis() - t0);
            runService.usage(runId, "GRADING", tokens, teacherId);
            runService.finish(runId, "SUCCESS", "LLM", tokens, null);
        }

        out.put("runId", runId);
        out.put("suggestions", listBySubmission(submissionId));
        out.put("skipped", skipped);
        out.put("tokens", tokens);
        out.put("cost", BigDecimal.valueOf(props.costOf(tokens)));
        out.put("message", suggestions.isEmpty() && !skipped.isEmpty()
                ? "模型没有返回可用建议，请重试或直接人工评分"
                : "已生成 " + suggestions.size() + " 条建议（AI 建议、教师定稿）");
        return out;
    }

    // ============================================================ 列表 / 采纳
    public List<Map<String, Object>> list(Long submissionId, Long teacherId) {
        Map<String, Object> sub = one("select id, paper_id from submission where id = ?", submissionId);
        if (sub == null) {
            throw new BizException("提交记录不存在");
        }
        paperService.requireMyPaper(lng(sub.get("paper_id")), teacherId);
        return listBySubmission(submissionId);
    }

    /**
     * 采纳建议：只记"教师最终用了多少分、评语改成了什么"，**不写分数**。
     * 写分仍由教师点「保存批改」走原有批改接口，保证判卷人与审计链路不变。
     */
    public Map<String, Object> apply(Long suggestionId, Map<String, Object> body, Long teacherId) {
        Map<String, Object> row = one("""
                select g.id, g.submission_id, g.full_score, g.suggested_score, g.comment, s.paper_id
                from ai_grading_suggest g join submission s on s.id = g.submission_id
                where g.id = ?
                """, suggestionId);
        if (row == null) {
            throw new BizException("建议不存在");
        }
        paperService.requireMyPaper(lng(row.get("paper_id")), teacherId);
        BigDecimal full = dec(row.get("full_score"));
        BigDecimal score = body.get("score") == null ? dec(row.get("suggested_score")) : dec(body.get("score"));
        if (score.signum() < 0) {
            score = BigDecimal.ZERO;
        }
        if (score.compareTo(full) > 0) {
            score = full;
        }
        String comment = body.get("comment") == null ? str(row.get("comment"))
                : String.valueOf(body.get("comment")).trim();
        if (comment.length() > 500) {
            comment = comment.substring(0, 499) + "…";
        }
        jdbc.update("""
                update ai_grading_suggest
                set status = 'APPLIED', final_score = ?, final_comment = ?, reviewer_id = ?, review_time = ?
                where id = ?
                """, score, comment, teacherId, LocalDateTime.now(), suggestionId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", suggestionId);
        out.put("status", "APPLIED");
        out.put("finalScore", score);
        out.put("finalComment", comment);
        out.put("hint", "建议已采纳，请在页面上点「保存批改」才真正写入分数");
        return out;
    }

    // ============================================================ 提示词与解析
    private static final String SYSTEM_PROMPT = """
            你是课程批改助手，只对给出的主观题给"建议分 + 评语"，最终分数由教师决定。
            规则：
            1. 严格按【参考要点】给分，要点答到几条给几条的分；没有参考要点的题只给分档（不给细抠）；
            2. 分数必须是 0 到满分之间的数字，可以带 0.5；不得超过满分；
            3. 评语 2 句以内，先说答对的部分，再指出缺失的要点；不要写"你很棒""继续努力"这类空话；
            4. 不要在评语里编造数字（例如"你答对了 3 个要点"），只能用满分、得分、参考要点里出现的内容；
            5. 不要输出 Markdown（不要星号、井号、表格、代码块），严格按下面的行格式输出，每题一段：
            【题目1】
            得分：3
            评语：……
            要点：……（可选，一句话说明怎么扣的分）
            """;

    private String buildPrompt(Paper paper, List<Map<String, Object>> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("课程作业：").append(paper.getPaperName()).append("（满分 ").append(paper.getFullScore()).append(" 分）\n");
        sb.append("\n【待批主观题】\n");
        int i = 1;
        for (Map<String, Object> r : items) {
            sb.append("题目").append(i++).append("（").append(qTypeLabel(str(r.get("q_type"))))
                    .append("，满分 ").append(dec(r.get("full_score"))).append(" 分）\n");
            sb.append("题干：").append(str(r.get("stem"))).append('\n');
            String ref = plainRef(r.get("std_json"));
            sb.append("参考要点：").append(ref.isBlank() ? "（本题没有给参考要点，请只给分档建议）" : ref).append('\n');
            String analysis = str(r.get("analysis"));
            if (!analysis.isBlank()) {
                sb.append("评分说明：").append(analysis).append('\n');
            }
            sb.append("学生作答：").append(plainAnswer(r.get("stu_json"))).append("\n\n");
        }
        sb.append("请按约定格式逐题输出建议分与评语，共 ").append(items.size()).append(" 题。");
        return sb.toString();
    }

    /** 解析「【题目N】得分/评语/要点」 */
    private List<Map<String, Object>> parse(String raw) {
        String text = ReportComposer.stripReasoning(raw == null ? "" : raw);
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Object> cur = null;
        String key = null;
        StringBuilder buf = new StringBuilder();
        for (String line : text.split("\\r?\\n")) {
            String s = line.trim();
            if (s.isEmpty()) {
                continue;
            }
            if (s.matches("^[【\\[]?\\s*题目\\s*\\d+\\s*[】\\]].*$")) {
                cur = flush(out, cur, key, buf);
                key = null;
                continue;
            }
            if (cur == null) {
                if (!s.startsWith("得分")) {
                    continue;
                }
                cur = new LinkedHashMap<>();
            }
            int idx = indexOfColon(s);
            if (idx > 0 && idx <= 4) {
                String k = s.substring(0, idx).replace("：", "").replace(":", "").trim();
                if (Set.of("得分", "评语", "要点").contains(k)) {
                    append(cur, key, buf);
                    key = k;
                    buf = new StringBuilder(s.substring(idx + 1).trim());
                    continue;
                }
            }
            if (key != null) {
                if (buf.length() > 0) {
                    buf.append('\n');
                }
                buf.append(s);
            }
        }
        flush(out, cur, key, buf);
        return out;
    }

    private Map<String, Object> flush(List<Map<String, Object>> out, Map<String, Object> cur, String key,
                                      StringBuilder buf) {
        if (cur == null) {
            return null;
        }
        append(cur, key, buf);
        if (cur.containsKey("score") || cur.containsKey("comment")) {
            out.add(cur);
        }
        return null;
    }

    private static void append(Map<String, Object> cur, String key, StringBuilder buf) {
        if (key == null || buf == null) {
            return;
        }
        String v = buf.toString().trim();
        switch (key) {
            case "得分" -> cur.put("score", firstNumber(v));
            case "评语" -> cur.put("comment", v);
            case "要点" -> cur.put("rubric", v);
            default -> {
                // 忽略
            }
        }
    }

    // ============================================================ 落库与视图
    private Map<String, Object> saveSuggestion(Map<String, Object> row, BigDecimal score, String comment,
                                               List<String> rubric, List<Map<String, Object>> checks,
                                               String source, Long runId, int tokens, Map<String, Object> parsed) {
        BigDecimal full = dec(row.get("full_score"));
        String stuText = plainAnswer(row.get("stu_json"));
        String refText = plainRef(row.get("std_json"));
        String checksJson = writeJson(Map.of("checks", checks));
        String rubricJson = writeJson(Map.of(
                "rubric", rubric,
                "modelRubric", parsed == null ? "" : str(parsed.get("rubric"))));
        jdbc.update("""
                delete from ai_grading_suggest where answer_id = ?
                """, lng(row.get("answer_id")));
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    insert into ai_grading_suggest(submission_id, answer_id, question_id, teaching_class_id, q_type,
                                                   full_score, student_answer, ref_answer, suggested_score, comment,
                                                   rubric_json, checks_json, source, model, prompt_version, run_id,
                                                   tokens, cost, status)
                    values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'SUGGESTED')
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, lng(row.get("submission_id")));
            ps.setLong(2, lng(row.get("answer_id")));
            ps.setLong(3, lng(row.get("question_id")));
            ps.setLong(4, lng(row.get("teaching_class_id")));
            ps.setString(5, str(row.get("q_type")));
            ps.setBigDecimal(6, full);
            ps.setString(7, truncate(stuText, 3000));
            ps.setString(8, truncate(refText, 1000));
            ps.setBigDecimal(9, score);
            ps.setString(10, truncate(comment, 500));
            ps.setString(11, rubricJson);
            ps.setString(12, checksJson);
            ps.setString(13, source);
            ps.setString(14, props.directModel());
            ps.setString(15, PROMPT_VERSION);
            if (runId == null) {
                ps.setNull(16, java.sql.Types.BIGINT);
            } else {
                ps.setLong(16, runId);
            }
            if (tokens <= 0) {
                ps.setNull(17, java.sql.Types.INTEGER);
                ps.setNull(18, java.sql.Types.DECIMAL);
            } else {
                ps.setInt(17, tokens);
                ps.setBigDecimal(18, BigDecimal.valueOf(props.costOf(tokens)));
            }
            return ps;
        }, kh);
        // 返回给前端的一条（带题干与学号无关的信息，避免把无关隐私带出去）
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", kh.getKey() == null ? null : kh.getKey().longValue());
        m.put("answerId", row.get("answer_id"));
        m.put("questionId", row.get("question_id"));
        m.put("qNo", row.get("q_no"));
        m.put("qType", row.get("q_type"));
        m.put("stem", row.get("stem"));
        m.put("fullScore", full);
        m.put("studentAnswer", stuText);
        m.put("refAnswer", refText);
        m.put("suggestedScore", score);
        m.put("comment", comment);
        m.put("checks", checks);
        m.put("source", source);
        m.put("status", "SUGGESTED");
        return m;
    }

    private List<Map<String, Object>> listBySubmission(Long submissionId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select g.id, g.answer_id, g.question_id, g.q_type, g.full_score, g.student_answer, g.ref_answer,
                       g.suggested_score, g.comment, g.rubric_json, g.checks_json, g.source, g.status,
                       g.final_score, g.final_comment, g.create_time, q.q_no, q.stem
                from ai_grading_suggest g
                         left join question q on q.id = g.question_id
                where g.submission_id = ?
                order by q.sort_no, q.q_no
                """, submissionId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.get("id"));
            m.put("answerId", r.get("answer_id"));
            m.put("questionId", r.get("question_id"));
            m.put("qNo", r.get("q_no"));
            m.put("qType", r.get("q_type"));
            m.put("stem", r.get("stem"));
            m.put("fullScore", r.get("full_score"));
            m.put("studentAnswer", r.get("student_answer"));
            m.put("refAnswer", r.get("ref_answer"));
            m.put("suggestedScore", r.get("suggested_score"));
            m.put("comment", r.get("comment"));
            m.put("checks", readChecks(r.get("checks_json")));
            m.put("modelRubric", readModelRubric(r.get("rubric_json")));
            m.put("source", r.get("source"));
            m.put("status", r.get("status"));
            m.put("finalScore", r.get("final_score"));
            m.put("finalComment", r.get("final_comment"));
            m.put("createTime", r.get("create_time"));
            out.add(m);
        }
        return out;
    }

    // ============================================================ 工具
    /** 三道校验（与出题的五道闸门保持同样的"可解释"风格） */
    private static List<Map<String, Object>> gateAll(boolean scoreOk, boolean digitsOk, boolean refOk, String detail) {
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(gate("SCORE_RANGE", "分数在满分内", scoreOk, scoreOk ? "ok" : detail));
        checks.add(gate("COMMENT_DIGITS", "评语不编造数字", digitsOk, digitsOk ? "ok" : detail));
        checks.add(gate("REF_EXISTS", "有参考要点", refOk, refOk ? "ok" : "本题没有参考要点，仅给分档建议"));
        return checks;
    }

    private static Map<String, Object> gate(String code, String name, boolean pass, String detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("name", name);
        m.put("pass", pass);
        m.put("detail", detail);
        return m;
    }

    private static String joinProblems(List<String> problems, List<String> badNumbers) {
        List<String> all = new ArrayList<>(problems);
        if (!badNumbers.isEmpty()) {
            all.add("评语出现无依据数字：" + String.join("、", badNumbers));
        }
        return String.join("；", all);
    }

    /** 评语里出现的数字，只允许是满分/得分，或参考要点/题干里出现过的 */
    private static List<String> badNumbers(String comment, BigDecimal full, String stdJson, String stem) {
        String allowedText = (stdJson == null ? "" : stdJson) + " " + (stem == null ? "" : stem);
        List<String> bad = new ArrayList<>();
        Matcher m = Pattern.compile("\\d+(?:\\.\\d+)?").matcher(comment);
        while (m.find()) {
            String num = m.group();
            BigDecimal v = new BigDecimal(num);
            if (v.compareTo(full) <= 0) {
                continue;
            }
            if (allowedText.contains(num)) {
                continue;
            }
            bad.add(num);
        }
        return bad;
    }

    /** 学生作答转纯文本：SHORT {"text":...} / BLANK {"answers":[...]} */
    private static String plainAnswer(Object json) {
        return plainAny(json, "text", "answers", "content");
    }

    /** 参考答案转纯文本（SHORT 用 ref，BLANK 用 answers） */
    private static String plainRef(Object json) {
        return plainAny(json, "ref", "answers", "text");
    }

    private static String plainAny(Object json, String first, String second, String third) {
        if (json == null) {
            return "";
        }
        String s = String.valueOf(json).trim();
        if (s.isEmpty() || "{}".equals(s) || "[]".equals(s)) {
            return "";
        }
        try {
            Object node = MAPPER.readValue(s, Object.class);
            if (node instanceof Map<?, ?> map) {
                for (String k : List.of(first, second, third)) {
                    Object v = map.get(k);
                    if (v == null) {
                        continue;
                    }
                    if (v instanceof List<?> list) {
                        List<String> parts = new ArrayList<>();
                        for (Object o : list) {
                            if (o != null && !String.valueOf(o).isBlank()) {
                                parts.add(String.valueOf(o).trim());
                            }
                        }
                        if (!parts.isEmpty()) {
                            return String.join(" ｜ ", parts);
                        }
                    } else if (!String.valueOf(v).isBlank()) {
                        return String.valueOf(v).trim();
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return s;
        }
    }

    private static Integer firstNumber(String v) {
        Matcher m = Pattern.compile("\\d+(?:\\.\\d+)?").matcher(v == null ? "" : v);
        if (!m.find()) {
            return null;
        }
        return Integer.valueOf(new BigDecimal(m.group()).intValue());
    }

    private static int indexOfColon(String s) {
        int a = s.indexOf('：');
        int b = s.indexOf(':');
        if (a < 0) {
            return b;
        }
        if (b < 0) {
            return a;
        }
        return Math.min(a, b);
    }

    private static String qTypeLabel(String t) {
        return switch (t == null ? "" : t) {
            case "SHORT" -> "简答";
            case "BLANK" -> "填空";
            default -> "主观题";
        };
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static Object readChecks(Object json) {
        String s = json == null ? "" : String.valueOf(json);
        try {
            Object node = MAPPER.readValue(s, Object.class);
            // 落库时存的是 {"checks":[...]}，返回给前端时摊平成数组更好用
            if (node instanceof Map<?, ?> map && map.get("checks") != null) {
                return map.get("checks");
            }
            return node;
        } catch (Exception e) {
            return null;
        }
    }

    private Object readModelRubric(Object json) {
        try {
            Object node = json == null ? null : MAPPER.readValue(String.valueOf(json), Object.class);
            if (node instanceof Map<?, ?> map) {
                return map.get("modelRubric");
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String writeJson(Object v) {
        try {
            return MAPPER.writeValueAsString(v);
        } catch (Exception e) {
            return null;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static BigDecimal dec(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal b) {
            return b;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(v).trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static Long lng(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }
}
