package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.*;

/**
 * 课程答疑（M11 的 G，配合 {@link KbService} 的 R）。
 *
 * <p>四条硬规则：</p>
 * <ol>
 *   <li>只依据检索到的片段回答，不引入片段外的知识；</li>
 *   <li>答案末尾必须给出「依据：文档名 第N段；…」，后端**逐条校验引用是否真实存在**；</li>
 *   <li>检索为空或最高相关度低于阈值 → **直接拒答**（不编）；</li>
 *   <li>模型不可用 → 退模板（只罗列命中片段，不生成结论）。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentQaService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbc;
    private final KbService kbService;
    private final RetrievalService retrievalService;
    private final DashScopeClient dashScopeClient;
    private final AiRunService runService;
    private final AiProperties props;

    // 拒答阈值（MIN_SCORE / MIN_LEX_SUPPORT / MIN_VECTOR_SCORE）统一放在 RetrievalService，
    // 因为 RAG 评测要复用同一套判据；这里不再重复定义，避免两把尺子。

    public Map<String, Object> ask(Long studentId, Long courseId, Long teachingClassId, String question) {
        return ask(studentId, courseId, teachingClassId, question, List.of(), null);
    }

    public Map<String, Object> ask(Long studentId, Long courseId, Long teachingClassId, String question,
                                   List<Map<String, Object>> history) {
        return ask(studentId, courseId, teachingClassId, question, history, null);
    }

    /** 带最近对话与流式回调；onDelta 为空时走原来的阻塞式生成。 */
    public Map<String, Object> ask(Long studentId, Long courseId, Long teachingClassId, String question,
                                   List<Map<String, Object>> history,
                                   java.util.function.Consumer<String> onDelta) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) {
            throw new BizException("问题不能为空");
        }
        if (q.length() > 300) {
            q = q.substring(0, 300);
        }
        if (courseId == null && teachingClassId != null) {
            List<Long> ids = jdbc.queryForList(
                    "select o.course_id from teaching_class tc join course_offering o on o.id = tc.offering_id where tc.id = ?",
                    Long.class, teachingClassId);
            courseId = ids.isEmpty() ? null : ids.get(0);
        }
        if (courseId == null) {
            throw new BizException("缺少 courseId 或 teachingClassId");
        }
        checkEnrollment(studentId, courseId, teachingClassId);
        String courseName = courseName(courseId);

        Long runId = runService.start("QA", "COURSE", courseId, studentId);
        long t0 = System.currentTimeMillis();
        int tokens = 0;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("question", q);
        out.put("runId", runId);
        out.put("courseId", courseId);
        out.put("courseName", courseName);

        // ---- R：检索（R2 混合检索：全文 + 向量 + RRF；向量没配置时自动只用全文路）
        RetrievalService.Outcome retrieval = retrievalService.retrieve(courseId, q, 4);
        List<Map<String, Object>> hits = RetrievalService.toMaps(retrieval.hits());
        Map<String, Object> retrievalDiag = retrievalService.diagnostics(retrieval);
        out.put("hits", hits);
        out.put("retrievalMode", retrieval.effective().name());
        if (retrieval.degraded() != null) {
            out.put("retrievalNote", retrieval.degraded());
        }
        double topScore = 0d;
        if (!hits.isEmpty()) {
            Object s = hits.get(0).get("score");
            topScore = s == null ? 0d : Double.parseDouble(String.valueOf(s));
        }
        double lexSupport = hits.isEmpty() ? 0d : lexicalSupport(q, String.valueOf(hits.get(0).get("content")));

        // 拒答判据统一由 RetrievalService.guard 给出：线上问答与 RAG 评测必须用同一把尺子，
        // 否则评测里"拒答准确率"会跟线上真实行为对不上。
        RetrievalService.Guard guard = retrievalService.guard(retrieval, q);
        boolean noEvidence = guard.refuse();

        // ---- 拒答分支：没检索到 / 相关度太低 / 连 2 字片段都对不上
        if (noEvidence) {
            String answer = "这个问题在《" + courseName + "》的资料里没有检索到足够可靠的依据，我先不给结论，避免给你错的信息。"
                    + "建议换个问法（比如直接问某一章的名词或公式），或把问题带到课堂上问任课老师。";
            Map<String, Object> res = finish(out, runId, studentId, courseId, teachingClassId, q,
                    answer, List.of(), hits.size(), topScore, true, "REFUSAL", "TEMPLATE", 0, t0,
                    retrieval.effective().name(), retrievalDiag);
            out.put("lexSupport", lexSupport);
            return res;
        }

        // ---- G：生成（带引用要求）
        String prompt = buildPrompt(courseName, q, history, hits);
        String systemPrompt = """
                你是课程答疑助手，只回答与给定课程资料相关的问题。
                规则：
                1. 只能使用【材料片段】里的内容，不得引入片段之外的知识，不得编造公式、页码或数据；
                2. 先给结论（2-3 句），再给必要步骤或要点；不使用 Markdown 星号、表格；
                3. 最后**必须**单独一行给出依据，格式严格为：
                   依据：文档名 第N段；文档名 第N段
                   （文档名与段号只能取自材料片段标题，不得编造）
                4. 若片段不足以回答，直接说明"当前资料不足以回答"，不要猜。
                """;
        String answer = null;
        String source = "TEMPLATE";
        if (props.directReady()) {
            DashScopeClient.ChatResult res = onDelta == null
                    ? dashScopeClient.chat(systemPrompt, prompt)
                    : dashScopeClient.streamChat(systemPrompt, prompt, onDelta);
            tokens = res.getTotalTokens();
            if (res.isOk()) {
                answer = res.getText();
                source = "LLM";
            } else {
                runService.step(runId, 1, "direct-chat", "qa", null, "FAIL", res.getError(),
                        System.currentTimeMillis() - t0);
            }
        }

        List<Map<String, Object>> citations = new ArrayList<>();
        boolean cited = false;
        if (answer != null) {
            String[] split = splitCitations(answer);
            String body = split[0];
            List<String> declared = parseDeclared(split[1]);
            citations = validateCitations(declared, hits);
            cited = !citations.isEmpty();
            if (!cited) {
                // 引用校验失败：用规则模板兜底（只列片段，不生成结论）
                answer = templateAnswer(courseName, hits);
                source = "TEMPLATE";
                citations = autoCitations(hits);
                cited = true;
            } else {
                answer = body;
            }
            runService.step(runId, 2, "citation-check", "qa", split[1],
                    cited ? "PASS" : "FAIL",
                    cited ? null : "引用与检索结果不匹配，已退模板",
                    System.currentTimeMillis() - t0);
        } else {
            answer = templateAnswer(courseName, hits);
            citations = autoCitations(hits);
        }

        out.put("streamed", onDelta != null && "LLM".equals(source));
        Map<String, Object> res = finish(out, runId, studentId, courseId, teachingClassId, q, answer, citations,
                hits.size(), topScore, false, "OK", source, tokens, t0,
                retrieval.effective().name(), retrievalDiag);
        res.put("lexSupport", lexSupport);
        return res;
    }

    /**
     * 词面支撑度：把问题切成 2 字片段，看有多少比例出现在片段正文里。
     * 与 ngram 分值不同，这个比例不受语料规模影响，用来拦住"检索到东西但和问题无关"的情况。
     */
    private static double lexicalSupport(String question, String content) {
        if (question == null || content == null) {
            return 0d;
        }
        String q = question.replaceAll("[\\s\\p{Punct}\\u3000-\\u303f\\uff00-\\uffef]", "").toLowerCase();
        if (q.length() < 2) {
            return 1d; // 单字问题不做词面判断
        }
        Set<String> grams = new LinkedHashSet<>();
        for (int i = 0; i + 2 <= q.length(); i++) {
            grams.add(q.substring(i, i + 2));
        }
        if (grams.isEmpty()) {
            return 0d;
        }
        String body = content.toLowerCase();
        int hit = 0;
        for (String g : grams) {
            if (body.contains(g)) {
                hit++;
            }
        }
        return (double) hit / grams.size();
    }

    // ============================================================ 组装与落库
    private Map<String, Object> finish(Map<String, Object> out, Long runId, Long studentId, Long courseId,
                                       Long teachingClassId, String question, String answer,
                                       List<Map<String, Object>> citations, int hitCount, double topScore,
                                       boolean refused, String status, String source, int tokens, long t0,
                                       String retrievalMode, Map<String, Object> retrievalDiag) {
        out.put("answer", answer);
        out.put("citations", citations);
        out.put("refused", refused);
        out.put("source", source);
        out.put("status", status);
        out.put("hitCount", hitCount);
        out.put("topScore", topScore);
        out.put("retrievalJson", retrievalDiag);

        String citationsJson = "[]";
        String retrievalJson = null;
        try {
            citationsJson = MAPPER.writeValueAsString(citations);
        } catch (Exception ignore) {
            // 忽略
        }
        try {
            retrievalJson = MAPPER.writeValueAsString(retrievalDiag);
        } catch (Exception ignore) {
            // 忽略
        }
        try {
            jdbc.update("""
                    insert into ai_qa_log(student_id, course_id, teaching_class_id, question, answer,
                                          citations_json, hit_count, top_score, refused, retrieval_mode,
                                          retrieval_json, status, source, tokens, cost)
                    values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, studentId, courseId, teachingClassId, question,
                    answer == null ? null : (answer.length() > 4000 ? answer.substring(0, 3999) : answer),
                    citationsJson, hitCount, BigDecimal.valueOf(topScore), refused ? 1 : 0, retrievalMode,
                    retrievalJson, status, source, tokens,
                    BigDecimal.valueOf(props.costOf(tokens)));
        } catch (Exception e) {
            log.warn("[QA] 落库失败: {}", e.getMessage());
        }
        runService.usage(runId, "QA", tokens, studentId);
        runService.finish(runId, "REFUSAL".equals(status) ? "SUCCESS" : status, source, tokens, null);
        return out;
    }

    /** 学生只能问自己已选课程；教学班优先，否则按课程校验。 */
    private void checkEnrollment(Long studentId, Long courseId, Long teachingClassId) {
        Integer n;
        if (teachingClassId != null) {
            n = jdbc.queryForObject("""
                    select count(*) from enrollment
                    where student_id = ? and teaching_class_id = ? and status in ('ACTIVE','ENROLLED')
                    """, Integer.class, studentId, teachingClassId);
        } else {
            n = jdbc.queryForObject("""
                    select count(*) from enrollment e
                    join teaching_class tc on tc.id = e.teaching_class_id
                    join course_offering o on o.id = tc.offering_id
                    where e.student_id = ? and o.course_id = ? and e.status in ('ACTIVE','ENROLLED')
                    """, Integer.class, studentId, courseId);
        }
        if (n == null || n == 0) {
            throw new BizException(403, "只能提问自己已选的课程");
        }
    }

    // ============================================================ 提示词与模板
    private String buildPrompt(String courseName, String question, List<Map<String, Object>> history,
                               List<Map<String, Object>> hits) {
        StringBuilder sb = new StringBuilder();
        sb.append("课程：").append(courseName).append("\n");
        if (history != null && !history.isEmpty()) {
            sb.append("【最近对话】\n");
            int n = Math.max(0, history.size() - 5);
            for (int j = n; j < history.size(); j++) {
                Map<String, Object> h = history.get(j);
                String role = "user".equals(String.valueOf(h.get("role"))) ? "学生" : "助手";
                String text = String.valueOf(h.get("content") == null ? "" : h.get("content"));
                if (text.length() > 300) text = text.substring(0, 300);
                sb.append(role).append("：").append(text).append("\n");
            }
            sb.append("\n");
        }
        sb.append("学生问题：").append(question).append("\n\n【材料片段】\n");
        int i = 1;
        for (Map<String, Object> h : hits) {
            sb.append(i++).append("）").append(h.get("docName")).append(" 第").append(h.get("sectionNo")).append("段")
                    .append(h.get("pageNo") == null ? "" : "（课件第" + h.get("pageNo") + "页）")
                    .append("：\n").append(h.get("content")).append("\n\n");
        }
        sb.append("请依据以上片段回答，并在最后一行给出「依据：文档名 第N段」。");
        return sb.toString();
    }

    private String templateAnswer(String courseName, List<Map<String, Object>> hits) {
        StringBuilder sb = new StringBuilder();
        sb.append("在《").append(courseName).append("》的资料里查到以下内容（AI 暂不可用，这里只列依据、不生成结论）：\n");
        int i = 1;
        for (Map<String, Object> h : hits) {
            sb.append(i++).append("）").append(h.get("docName")).append(" 第").append(h.get("sectionNo")).append("段")
                    .append(h.get("pageNo") == null ? "" : "（第" + h.get("pageNo") + "页）")
                    .append("：").append(h.get("snippet")).append("\n");
        }
        sb.append("如需口径解释，建议直接问任课老师。");
        return sb.toString();
    }

    // ============================================================ 引用解析与校验
    /** 返回 [正文, 依据行] */
    private String[] splitCitations(String answer) {
        int idx = answer.lastIndexOf("依据");
        if (idx < 0) {
            return new String[]{answer.trim(), ""};
        }
        int colon = answer.indexOf('：', idx);
        if (colon < 0) {
            colon = answer.indexOf(':', idx);
        }
        if (colon < 0) {
            return new String[]{answer.trim(), ""};
        }
        String body = answer.substring(0, idx).trim();
        String decl = answer.substring(colon + 1).trim();
        if (body.isEmpty()) {
            return new String[]{answer.trim(), ""};
        }
        return new String[]{body, decl};
    }

    private List<String> parseDeclared(String decl) {
        if (decl == null || decl.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : decl.split("[；;]")) {
            String s = part.trim().replaceAll("^[\\s、,，]+", "");
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out.stream().limit(5).toList();
    }

    /** 把"文档名 第N段"与检索结果对齐；只要有一条对不上，整段引用作废 */
    private List<Map<String, Object>> validateCitations(List<String> declared, List<Map<String, Object>> hits) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (declared.isEmpty()) {
            return out;
        }
        for (String d : declared) {
            Map<String, Object> match = null;
            for (Map<String, Object> h : hits) {
                String doc = String.valueOf(h.get("docName"));
                Integer no = h.get("sectionNo") == null ? null : Integer.parseInt(String.valueOf(h.get("sectionNo")));
                if (no == null) {
                    continue;
                }
                boolean docOk = d.contains(doc) || doc.contains(d.replaceAll("第\\d+段", "").trim());
                boolean noOk = d.contains("第" + no + "段");
                if (docOk && noOk) {
                    match = h;
                    break;
                }
            }
            if (match == null) {
                return new ArrayList<>();
            }
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("docName", match.get("docName"));
            c.put("sectionNo", match.get("sectionNo"));
            c.put("pageNo", match.get("pageNo"));
            out.add(c);
        }
        return out;
    }

    private List<Map<String, Object>> autoCitations(List<Map<String, Object>> hits) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> h : hits.stream().limit(3).toList()) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("docName", h.get("docName"));
            c.put("sectionNo", h.get("sectionNo"));
            c.put("pageNo", h.get("pageNo"));
            out.add(c);
        }
        return out;
    }

    private String courseName(Long courseId) {
        List<String> names = jdbc.queryForList("select course_name from course where id = ?", String.class, courseId);
        return names.isEmpty() ? "本课程" : names.get(0);
    }
}

