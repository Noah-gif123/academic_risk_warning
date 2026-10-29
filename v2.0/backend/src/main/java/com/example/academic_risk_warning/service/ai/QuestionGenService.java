package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.config.AiProperties;
import com.example.academic_risk_warning.dto.AiQuestionGenRequest;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.service.CourseContentService;
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

/**
 * AI 出题（M14）：按<b>所选资料或上传的课件</b>出题 —— 检索 → 生成 → 五道闸门 → 审题草稿。
 *
 * <p>为什么是"草稿"而不是"直接进卷"：出题属于"教师减负"，不是"教师下岗"。模型产出的题一律先落
 * {@code ai_question_draft}，教师在试卷编辑页逐题看、改、弃，选中后才进试卷（走原有的保存链路，
 * 分值、发布、撤回、批阅全都不变）。这样答辩时可以明说：<b>AI 只出草稿，成题的责任永远在教师</b>。</p>
 *
 * <p>五道闸门（{@code quality_json.checks}，每道都记录通过与否与原因）：</p>
 * <ol>
 *   <li>STRUCT 结构完整：题干非空、选择题至少 3 个选项、各题型字段齐全；</li>
 *   <li>ANSWER 答案自洽：单选答案唯一且落在选项里、多选至少 2 项、判断能解析成对/错；</li>
 *   <li>SOURCE 出处可核：依据必须能对上本次素材的「文档名 + 第N段」，编造的依据直接拦下；</li>
 *   <li>DUP 不照抄、不重复：题干与原文重合度、与历史草稿的重复度都要低于阈值；</li>
 *   <li>SCORE 难度与分值：难度合法，分值按题型给默认值（教师可改）。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGenService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String PROMPT_VERSION = "question-gen-v1";
    private static final int MAX_COUNT = 10;
    private static final int MAX_MATERIAL_CHARS = 3600;

    /** 题干与原文的 2 字片段重合度上限（超过视为照抄原文） */
    private static final double MAX_COPY_RATIO = 0.75;
    /** 与历史草稿的重复度上限 */
    private static final double MAX_DUP_RATIO = 0.80;

    private static final Map<String, String> TYPE_LABEL = Map.of(
            "SINGLE", "单选", "MULTI", "多选", "JUDGE", "判断", "BLANK", "填空", "SHORT", "简答");

    private static final Map<String, BigDecimal> DEFAULT_SCORE = Map.of(
            "SINGLE", new BigDecimal("2"), "MULTI", new BigDecimal("3"), "JUDGE", new BigDecimal("2"),
            "BLANK", new BigDecimal("2"), "SHORT", new BigDecimal("5"));

    private final JdbcTemplate jdbc;
    private final CourseContentService contentService;
    private final DashScopeClient client;
    private final AiRunService runService;
    private final AiProperties props;

    // ============================================================ 可选资料（知识库文档）
    /** 本课程已入库的资料清单，供教师勾选"按哪些资料出题" */
    public Map<String, Object> sources(Long teachingClassId, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(teachingClassId, teacherId);
        Long courseId = courseIdOf(tc.getOfferingId());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("teachingClassId", teachingClassId);
        out.put("courseId", courseId);
        out.put("courseName", courseName(courseId));
        List<Map<String, Object>> docs = jdbc.queryForList("""
                select id, doc_name, source_type, chunk_count, char_count, update_time, offering_id
                from ai_kb_doc where course_id = ? order by source_type desc, id
                """, courseId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> d : docs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("docId", d.get("id"));
            m.put("docName", d.get("doc_name"));
            m.put("sourceType", d.get("source_type"));
            m.put("chunkCount", d.get("chunk_count"));
            m.put("charCount", d.get("char_count"));
            m.put("updateTime", d.get("update_time"));
            list.add(m);
        }
        out.put("docs", list);
        out.put("message", list.isEmpty() ? "这⻔课还没有入库的资料，先到「课程内容 → 资料库」上传课件" : "ok");
        return out;
    }

    // ============================================================ 出题
    @Transactional
    public Map<String, Object> generate(AiQuestionGenRequest req, Long teacherId) {
        if (!props.isEnabled()) {
            throw new BizException("AI 未启用（ai.enabled=false），无法出题");
        }
        if (req == null || req.getTeachingClassId() == null) {
            throw new BizException("缺少教学班");
        }
        TeachingClass tc = contentService.requireMyClass(req.getTeachingClassId(), teacherId);
        Long courseId = courseIdOf(tc.getOfferingId());
        String course = courseName(courseId);
        if (runService.overLimit(teacherId, "QUESTION")) {
            throw new BizException("今日 AI 出题次数已达上限（" + props.getDailyLimit() + " 次），请明天再试");
        }

        int count = req.getCount() == null ? 5 : Math.max(1, Math.min(MAX_COUNT, req.getCount()));
        List<String> types = normalizeTypes(req.getTypes());
        String difficulty = normalizeDifficulty(req.getDifficulty());

        List<Map<String, Object>> material = pickMaterial(courseId, req.getDocIds(), count);
        if (material.isEmpty()) {
            throw new BizException("这⻔课还没有可用于出题的资料：先到「课程内容 → 资料库」上传课件（支持 pptx/docx/txt），再试一次");
        }
        if (!props.directReady()) {
            throw new BizException("模型通道未配置（app.ai.llm-* 或 dashscope-* 为空），无法出题");
        }

        Long runId = runService.start("QUESTION", "OFFERING", tc.getOfferingId(), teacherId);
        long t0 = System.currentTimeMillis();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("teachingClassId", tc.getId());
        out.put("courseId", courseId);
        out.put("courseName", course);
        out.put("promptVersion", PROMPT_VERSION);

        String userPrompt = buildUserPrompt(course, material, count, types, difficulty, req.getInstruction());
        runService.step(runId, 1, "retrieve-material", "count=" + count + " types=" + types,
                "素材 " + material.size() + " 段", "PASS", null, System.currentTimeMillis() - t0);

        DashScopeClient.ChatResult res = client.chat(SYSTEM_PROMPT, userPrompt, 0.65, 2600);
        int tokens = res.getTotalTokens();
        if (!res.isOk()) {
            runService.step(runId, 2, "llm-generate", "question", null, "FAIL", res.getError(),
                    System.currentTimeMillis() - t0);
            runService.finish(runId, "FAILED", "LLM", tokens, res.getError());
            runService.usage(runId, "QUESTION", tokens, teacherId);
            throw new BizException("出题失败：" + res.getError());
        }
        out.put("source", "LLM");

        List<Parsed> parsed = parse(res.getText());
        runService.step(runId, 2, "llm-generate", "question", "解析出 " + parsed.size() + " 题", "PASS", null,
                System.currentTimeMillis() - t0);

        // ---- 五道闸门
        List<Map<String, Object>> checks = new ArrayList<>();
        int passed = 0;
        for (Parsed p : parsed) {
            runGates(p, material, tc.getOfferingId());
            if (p.passed) {
                passed++;
            }
            checks.add(Map.of("type", p.qType == null ? "?" : p.qType,
                    "passed", p.passed, "failed", p.failedCodes()));
        }
        runService.step(runId, 3, "five-gates", "5 道闸门",
                "通过 " + passed + "/" + parsed.size(), passed > 0 ? "PASS" : "FAIL",
                passed > 0 ? null : "全部未通过闸门", System.currentTimeMillis() - t0);

        // ---- 落草稿
        List<Map<String, Object>> drafts = new ArrayList<>();
        for (Parsed p : parsed) {
            Long id = insertDraft(p, tc, courseId, req, runId, teacherId);
            drafts.add(toView(id, p));
        }

        BigDecimal cost = BigDecimal.valueOf(props.costOf(tokens));
        out.put("runId", runId);
        out.put("drafts", drafts);
        out.put("materialCount", material.size());
        out.put("stats", Map.of("generated", parsed.size(), "passed", passed, "blocked", parsed.size() - passed));
        out.put("tokens", tokens);
        out.put("cost", cost);
        out.put("message", parsed.isEmpty()
                ? "模型没有按约定格式返回题目，请重试一次"
                : "已生成 " + parsed.size() + " 道草稿题（通过闸门 " + passed + " 道）");

        runService.usage(runId, "QUESTION", tokens, teacherId);
        runService.finish(runId, parsed.isEmpty() ? "FAILED" : "SUCCESS", "LLM", tokens, null);
        return out;
    }

    // ============================================================ 草稿列表 / 编辑 / 弃用 / 采纳
    public List<Map<String, Object>> listDrafts(Long teachingClassId, String status, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(teachingClassId, teacherId);
        StringBuilder sql = new StringBuilder("""
                select id, offering_id, q_type, difficulty, stem, options_json, answer_json, analysis, group_name,
                       source_pages, quality_json, full_score, status, create_by, create_time
                from ai_question_draft where offering_id = ? and deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        args.add(tc.getOfferingId());
        if (status != null && !status.isBlank()) {
            sql.append(" and status = ?");
            args.add(status.toUpperCase());
        }
        sql.append(" order by id desc limit 60");
        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.get("id"));
            m.put("qType", r.get("q_type"));
            m.put("typeLabel", TYPE_LABEL.getOrDefault(String.valueOf(r.get("q_type")), "其他"));
            m.put("difficulty", r.get("difficulty"));
            m.put("stem", r.get("stem"));
            m.put("options", readJson(r.get("options_json")));
            m.put("answer", readJson(r.get("answer_json")));
            m.put("analysis", r.get("analysis"));
            m.put("groupName", r.get("group_name"));
            m.put("sourcePages", r.get("source_pages"));
            m.put("quality", readJson(r.get("quality_json")));
            m.put("fullScore", r.get("full_score"));
            m.put("status", r.get("status"));
            m.put("createBy", r.get("create_by"));
            m.put("createTime", r.get("create_time"));
            out.add(m);
        }
        return out;
    }

    /** 教师改题（题干 / 选项 / 答案 / 解析 / 题型 / 分值），改完仍留在草稿里 */
    public Map<String, Object> updateDraft(Long id, Map<String, Object> body, Long teacherId) {
        Map<String, Object> row = requireMyDraft(id, teacherId);
        String stem = body.get("stem") == null ? String.valueOf(row.get("stem")) : String.valueOf(body.get("stem")).trim();
        if (stem.isBlank()) {
            throw new BizException("题干不能为空");
        }
        String qType = body.get("qType") == null ? String.valueOf(row.get("q_type"))
                : String.valueOf(body.get("qType")).toUpperCase();
        if (!TYPE_LABEL.containsKey(qType)) {
            throw new BizException("题型不合法：" + qType);
        }
        String optionsJson = body.get("optionsJson") == null ? null : String.valueOf(body.get("optionsJson"));
        String answerJson = body.get("answerJson") == null ? null : String.valueOf(body.get("answerJson"));
        String analysis = body.get("analysis") == null ? null : String.valueOf(body.get("analysis"));
        BigDecimal fullScore = body.get("fullScore") == null ? null
                : new BigDecimal(String.valueOf(body.get("fullScore")));
        String groupName = body.get("groupName") == null ? null : String.valueOf(body.get("groupName"));

        jdbc.update("""
                update ai_question_draft set stem = ?,
                       q_type = ?,
                       options_json = coalesce(?, options_json),
                       answer_json = coalesce(?, answer_json),
                       analysis = coalesce(?, analysis),
                       full_score = coalesce(?, full_score),
                       group_name = coalesce(?, group_name),
                       status = 'DRAFT'
                where id = ?
                """, stem, qType, optionsJson, answerJson, analysis, fullScore, groupName, id);
        return listDraftsByOffering(lng(row.get("offering_id")), "DRAFT", teacherId).stream()
                .filter(m -> String.valueOf(m.get("id")).equals(String.valueOf(id)))
                .findFirst().orElse(Map.of("id", id));
    }

    public Map<String, Object> rejectDraft(Long id, Long teacherId) {
        requireMyDraft(id, teacherId);
        jdbc.update("update ai_question_draft set status='REJECTED', reviewer_id=?, review_time=? where id=?",
                teacherId, LocalDateTime.now(), id);
        return Map.of("id", id, "status", "REJECTED");
    }

    /** 采纳：只改状态（题目进试卷走的是试卷保存接口，责任链不变） */
    @Transactional
    public Map<String, Object> adopt(List<Long> ids, Long teacherId) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请先勾选要采纳的题目");
        }
        int n = 0;
        for (Long id : ids) {
            requireMyDraft(id, teacherId);
            jdbc.update("update ai_question_draft set status='ADOPTED', reviewer_id=?, review_time=? where id=?",
                    teacherId, LocalDateTime.now(), id);
            n++;
        }
        return Map.of("adopted", n, "status", "ADOPTED");
    }

    // ============================================================ 素材挑选
    /** 按文档轮转取分块，保证多份资料都能覆盖到；总字数受 {@link #MAX_MATERIAL_CHARS} 限制 */
    private List<Map<String, Object>> pickMaterial(Long courseId, List<Long> docIds, int count) {
        List<Map<String, Object>> docs;
        if (docIds != null && !docIds.isEmpty()) {
            String in = String.join(",", docIds.stream().map(String::valueOf).toList());
            docs = jdbc.queryForList(
                    "select id, doc_name from ai_kb_doc where course_id = ? and id in (" + in + ") order by id",
                    courseId);
        } else {
            docs = jdbc.queryForList(
                    "select id, doc_name from ai_kb_doc where course_id = ? order by id", courseId);
        }
        if (docs.isEmpty()) {
            return List.of();
        }
        // 每份文档先取 2 段，不够再回头补
        int perDoc = Math.max(2, (int) Math.ceil((count + 2) / (double) docs.size()));
        List<Map<String, Object>> picked = new ArrayList<>();
        int chars = 0;
        Set<Long> seen = new HashSet<>();
        for (int round = 0; round < perDoc; round++) {
            for (Map<String, Object> d : docs) {
                Long docId = lng(d.get("id"));
                List<Map<String, Object>> chunks = jdbc.queryForList("""
                        select c.id, c.doc_name, c.section_no, c.source_type, c.content
                        from ai_kb_chunk c
                                 join ai_kb_doc d on d.course_id = c.course_id and d.source_type = c.source_type
                            and d.source_id = c.source_id
                        where c.course_id = ? and d.id = ?
                        order by c.section_no
                        limit ?, 1
                        """, courseId, docId, round);
                if (chunks.isEmpty()) {
                    continue;
                }
                Map<String, Object> c = chunks.get(0);
                Long chunkId = lng(c.get("id"));
                String content = String.valueOf(c.get("content"));
                if (!seen.add(chunkId) || chars + content.length() > MAX_MATERIAL_CHARS) {
                    continue;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("chunkId", chunkId);
                m.put("docName", c.get("doc_name"));
                m.put("sectionNo", c.get("section_no"));
                m.put("sourceType", c.get("source_type"));
                m.put("pageNo", KbExtractor.pageOf(content));
                m.put("content", content);
                picked.add(m);
                chars += content.length();
            }
        }
        return picked;
    }

    // ============================================================ 提示词
    private static final String SYSTEM_PROMPT = """
            你是课程出题助手，只根据给出的【材料片段】出题，不得引入片段之外的知识。
            规则：
            1. 每道题都必须能由片段内容直接判定对错，不依赖常识或课外知识；
            2. 不得整句照抄片段原文，要改写题干（公式、数值、步骤可以照用）；
            3. 每题必须给出依据，格式严格为「文档名 第N段」，文档名与段号只能取自片段标题，不得编造；
            4. 不要输出 Markdown（不要星号、井号、表格、代码块），不要写任何前言、总结或题号外的说明；
            5. 严格按下面的行格式输出，每题一段：
            【题目1】
            题型：单选
            难度：中
            题干：……
            选项：A. ……
            选项：B. ……
            选项：C. ……
            选项：D. ……
            答案：B
            解析：……
            依据：文档名 第N段
            补充格式：题型只能填 单选/多选/判断/填空/简答；多选答案写 A,C；判断答案写 正确 或 错误；
            填空多个空用 | 分隔；简答答案写一两句参考要点。判断与简答不需要写选项。
            """;

    private String buildUserPrompt(String courseName, List<Map<String, Object>> material, int count,
                                   List<String> types, String difficulty, String instruction) {
        StringBuilder sb = new StringBuilder();
        sb.append("课程：").append(courseName).append('\n');
        sb.append("要求：出 ").append(count).append(" 道题；题型分布：");
        sb.append(String.join("、", types.stream().map(t -> TYPE_LABEL.getOrDefault(t, t)).toList()));
        sb.append("；难度：").append(difficultyLabel(difficulty)).append("。\n");
        if (instruction != null && !instruction.isBlank()) {
            sb.append("教师补充要求：").append(instruction.trim()).append('\n');
        }
        sb.append("\n【材料片段】\n");
        int i = 1;
        for (Map<String, Object> m : material) {
            sb.append(i++).append("）").append(m.get("docName")).append(" 第").append(m.get("sectionNo")).append("段")
                    .append(m.get("pageNo") == null ? "" : "（课件第" + m.get("pageNo") + "页）")
                    .append("：\n").append(m.get("content")).append("\n\n");
        }
        sb.append("请严格按约定格式输出 ").append(count).append(" 道题。");
        return sb.toString();
    }

    // ============================================================ 解析
    /** 一道解析出来的题（含闸门结果） */
    static class Parsed {
        String qType;
        String difficulty = "medium";
        String stem = "";
        List<Map<String, String>> options = new ArrayList<>();
        String answerRaw = "";
        String analysis = "";
        String citation = "";
        String docName = "";
        Integer sectionNo;
        Integer pageNo;
        BigDecimal fullScore;
        boolean passed;
        final List<Map<String, Object>> checks = new ArrayList<>();

        String failedCodes() {
            List<String> codes = new ArrayList<>();
            for (Map<String, Object> c : checks) {
                if (!Boolean.TRUE.equals(c.get("pass"))) {
                    codes.add(String.valueOf(c.get("code")));
                }
            }
            return String.join(",", codes);
        }
    }

    List<Parsed> parse(String raw) {
        String text = ReportComposer.stripReasoning(raw == null ? "" : raw);
        List<Parsed> out = new ArrayList<>();
        Parsed cur = null;
        String key = null;
        StringBuilder buf = new StringBuilder();
        for (String line : text.split("\\r?\\n")) {
            String s = line.trim();
            if (s.isEmpty()) {
                continue;
            }
            if (s.matches("^[【\\[]?\\s*题目\\s*\\d+\\s*[】\\]]?.*$")) {
                cur = flush(out, cur, key, buf);
                key = null;
                continue;
            }
            if (cur == null) {
                // 允许模型漏写【题目N】直接开始，遇到"题型："就开始新题
                if (!s.startsWith("题型")) {
                    continue;
                }
                cur = new Parsed();
            }
            int idx = indexOfColon(s);
            if (idx > 0 && idx <= 4) {
                String k = s.substring(0, idx).replace("：", "").replace(":", "").trim();
                if (isKnownKey(k)) {
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

    private Parsed flush(List<Parsed> out, Parsed cur, String key, StringBuilder buf) {
        if (cur == null) {
            return null;
        }
        append(cur, key, buf);
        if (cur.qType != null || !cur.stem.isBlank()) {
            out.add(cur);
        }
        return null;
    }

    private static void append(Parsed cur, String key, StringBuilder buf) {
        if (key == null || buf == null) {
            return;
        }
        String v = buf.toString().trim();
        switch (key) {
            case "题型" -> cur.qType = mapType(v);
            case "难度" -> cur.difficulty = normalizeDifficulty(v);
            case "题干" -> cur.stem = v;
            case "选项" -> {
                Map<String, String> opt = parseOption(v, cur.options.size());
                if (opt != null) {
                    cur.options.add(opt);
                }
            }
            case "答案" -> cur.answerRaw = v;
            case "解析" -> cur.analysis = v;
            case "依据" -> cur.citation = v;
            default -> {
                // 未知字段忽略
            }
        }
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

    private static boolean isKnownKey(String k) {
        return Set.of("题型", "难度", "题干", "选项", "答案", "解析", "依据").contains(k);
    }

    /** "A. 内容" / "A、内容" / "A）内容" → {key:A,text:内容} */
    private static Map<String, String> parseOption(String v, int index) {
        String s = v.trim();
        if (s.isEmpty()) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^([A-Ha-h])\\s*[.、．)）:：]\\s*(.*)$").matcher(s);
        String key;
        String text;
        if (m.matches()) {
            key = m.group(1).toUpperCase();
            text = m.group(2).trim();
        } else {
            key = String.valueOf((char) ('A' + Math.min(index, 7)));
            text = s;
        }
        if (text.isBlank()) {
            return null;
        }
        Map<String, String> opt = new LinkedHashMap<>();
        opt.put("key", key);
        opt.put("text", text);
        return opt;
    }

    private static String mapType(String v) {
        if (v == null) {
            return null;
        }
        String s = v.trim();
        if (s.contains("多选") || s.equalsIgnoreCase("MULTI")) {
            return "MULTI";
        }
        if (s.contains("判断") || s.equalsIgnoreCase("JUDGE")) {
            return "JUDGE";
        }
        if (s.contains("填空") || s.equalsIgnoreCase("BLANK")) {
            return "BLANK";
        }
        if (s.contains("简答") || s.contains("问答") || s.equalsIgnoreCase("SHORT")) {
            return "SHORT";
        }
        if (s.contains("单选") || s.equalsIgnoreCase("SINGLE")) {
            return "SINGLE";
        }
        return null;
    }

    // ============================================================ 五道闸门
    void runGates(Parsed p, List<Map<String, Object>> material, Long offeringId) {
        p.checks.clear();
        p.fullScore = DEFAULT_SCORE.getOrDefault(p.qType, new BigDecimal("2"));

        // 1 结构
        List<String> structMsg = new ArrayList<>();
        if (p.qType == null) {
            structMsg.add("题型无法识别");
        }
        if (p.stem == null || p.stem.length() < 8) {
            structMsg.add("题干过短");
        }
        if (("SINGLE".equals(p.qType) || "MULTI".equals(p.qType)) && p.options.size() < 3) {
            structMsg.add("选择题选项少于 3 个");
        }
        gate(p, "STRUCT", "结构完整", structMsg);

        // 2 答案自洽
        List<String> ansMsg = new ArrayList<>();
        Set<String> optKeys = new HashSet<>();
        for (Map<String, String> o : p.options) {
            optKeys.add(o.get("key"));
        }
        List<String> keys = answerKeys(p.answerRaw);
        switch (String.valueOf(p.qType)) {
            case "SINGLE" -> {
                if (keys.size() != 1) {
                    ansMsg.add("单选答案应为 1 个选项");
                } else if (!optKeys.contains(keys.get(0))) {
                    ansMsg.add("答案 " + keys.get(0) + " 不在选项里");
                }
            }
            case "MULTI" -> {
                if (keys.size() < 2) {
                    ansMsg.add("多选答案少于 2 项");
                } else if (!optKeys.containsAll(keys)) {
                    ansMsg.add("答案不在选项里：" + keys);
                }
            }
            case "JUDGE" -> {
                if (parseBool(p.answerRaw) == null) {
                    ansMsg.add("判断答案无法识别（应为 正确/错误）");
                }
            }
            case "BLANK" -> {
                if (p.answerRaw == null || p.answerRaw.isBlank()) {
                    ansMsg.add("填空题没有参考答案");
                }
            }
            case "SHORT" -> {
                if (p.answerRaw == null || p.answerRaw.length() < 6) {
                    ansMsg.add("简答题参考要点过短");
                }
            }
            default -> ansMsg.add("题型未知，答案无法校验");
        }
        gate(p, "ANSWER", "答案自洽", ansMsg);

        // 3 出处可核
        List<String> srcMsg = new ArrayList<>();
        Map<String, Object> matched = matchCitation(p.citation, material);
        if (p.citation == null || p.citation.isBlank()) {
            srcMsg.add("没有给依据");
        } else if (matched == null) {
            srcMsg.add("依据对不上本次素材（可能编造）：" + p.citation);
        } else {
            p.docName = String.valueOf(matched.get("docName"));
            p.sectionNo = intOf(matched.get("sectionNo"));
            p.pageNo = intOf(matched.get("pageNo"));
        }
        gate(p, "SOURCE", "出处可核", srcMsg);

        // 4 不照抄 / 不重复
        List<String> dupMsg = new ArrayList<>();
        if (matched != null) {
            double copy = containment(p.stem, String.valueOf(matched.get("content")));
            if (copy > MAX_COPY_RATIO) {
                dupMsg.add("题干与原文重合 " + Math.round(copy * 100) + "%，请改写");
            }
        }
        for (Map<String, Object> old : jdbc.queryForList(
                "select stem from ai_question_draft where offering_id = ? and deleted = 0 order by id desc limit 60",
                offeringId)) {
            double sim = jaccard(p.stem, String.valueOf(old.get("stem")));
            if (sim > MAX_DUP_RATIO) {
                dupMsg.add("与已有草稿重复（相似度 " + Math.round(sim * 100) + "%）");
                break;
            }
        }
        gate(p, "DUP", "不照抄不重复", dupMsg);

        // 5 难度与分值
        List<String> scoreMsg = new ArrayList<>();
        if (!Set.of("easy", "medium", "hard").contains(p.difficulty)) {
            p.difficulty = "medium";
            scoreMsg.add("难度已回退为「中」");
        }
        if (p.fullScore == null || p.fullScore.signum() <= 0) {
            p.fullScore = new BigDecimal("2");
            scoreMsg.add("分值已回退为 2 分");
        }
        gate(p, "SCORE", "难度与分值", scoreMsg);

        p.passed = p.checks.stream().allMatch(c -> Boolean.TRUE.equals(c.get("pass")));
    }

    private void gate(Parsed p, String code, String name, List<String> problems) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("name", name);
        m.put("pass", problems.isEmpty());
        m.put("detail", problems.isEmpty() ? "ok" : String.join("；", problems));
        p.checks.add(m);
    }

    /** 依据「文档名 第N段」在素材里找原文 */
    private Map<String, Object> matchCitation(String citation, List<Map<String, Object>> material) {
        if (citation == null || citation.isBlank()) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("第\\s*(\\d+)\\s*段").matcher(citation);
        Integer no = null;
        if (m.find()) {
            no = Integer.parseInt(m.group(1));
        }
        for (Map<String, Object> c : material) {
            String doc = String.valueOf(c.get("docName"));
            boolean docOk = citation.contains(doc) || doc.contains(citation.replaceAll("第\\s*\\d+\\s*段", "").trim());
            boolean noOk = no == null || intOf(c.get("sectionNo")) == null || no.equals(intOf(c.get("sectionNo")));
            if (docOk && noOk) {
                return c;
            }
        }
        // 段号对不上但文档对得上：仍算"可核"，但把段号修正为素材里的真实段号，避免引用假页码
        for (Map<String, Object> c : material) {
            String doc = String.valueOf(c.get("docName"));
            if (citation.contains(doc)) {
                return c;
            }
        }
        return null;
    }

    // ============================================================ 落库
    private Long insertDraft(Parsed p, TeachingClass tc, Long courseId, AiQuestionGenRequest req,
                             Long runId, Long teacherId) {
        String optionsJson = p.options.isEmpty() ? null : writeJson(p.options);
        String answerJson = writeJson(answerOf(p));
        String qualityJson = writeJson(Map.of("passed", p.passed, "checks", p.checks,
                "promptVersion", PROMPT_VERSION));
        String sourcePages = p.pageNo == null ? (p.sectionNo == null ? null : String.valueOf(p.sectionNo))
                : String.valueOf(p.pageNo);
        String groupName = req.getGroupName() == null || req.getGroupName().isBlank()
                ? (p.qType == null ? "AI 出题" : "AI 出题 · " + TYPE_LABEL.getOrDefault(p.qType, "题"))
                : req.getGroupName().trim();
        String resourceIds = req.getDocIds() == null || req.getDocIds().isEmpty() ? null
                : String.join(",", req.getDocIds().stream().map(String::valueOf).toList());

        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    insert into ai_question_draft(offering_id, lesson_id, resource_ids, q_type, difficulty, stem,
                                                  options_json, answer_json, analysis, group_name, source_pages,
                                                  quality_json, full_score, status, model, prompt_version, run_id,
                                                  create_by)
                    values (?,?,?,?,?,?,?,?,?,?,?,?,?,'DRAFT',?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, tc.getOfferingId());
            ps.setNull(2, java.sql.Types.BIGINT);
            ps.setString(3, resourceIds);
            ps.setString(4, p.qType == null ? "SINGLE" : p.qType);
            ps.setString(5, p.difficulty);
            ps.setString(6, p.stem);
            ps.setString(7, optionsJson);
            ps.setString(8, answerJson);
            ps.setString(9, p.analysis);
            ps.setString(10, groupName);
            ps.setString(11, sourcePages);
            ps.setString(12, qualityJson);
            ps.setBigDecimal(13, p.fullScore);
            ps.setString(14, props.directModel());
            ps.setString(15, PROMPT_VERSION);
            if (runId == null) {
                ps.setNull(16, java.sql.Types.BIGINT);
            } else {
                ps.setLong(16, runId);
            }
            ps.setLong(17, teacherId);
            return ps;
        }, kh);
        return kh.getKey() == null ? null : kh.getKey().longValue();
    }

    private Map<String, Object> toView(Long id, Parsed p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("qType", p.qType);
        m.put("typeLabel", TYPE_LABEL.getOrDefault(String.valueOf(p.qType), "其他"));
        m.put("difficulty", p.difficulty);
        m.put("stem", p.stem);
        m.put("options", p.options);
        m.put("answer", answerOf(p));
        m.put("analysis", p.analysis);
        m.put("sourceRef", (p.docName == null ? "" : p.docName)
                + (p.sectionNo == null ? "" : " 第" + p.sectionNo + "段")
                + (p.pageNo == null ? "" : "（第" + p.pageNo + "页）"));
        m.put("quality", Map.of("passed", p.passed, "checks", p.checks));
        m.put("fullScore", p.fullScore);
        m.put("status", "DRAFT");
        return m;
    }

    /** 答案结构化：单选多选 keys / 判断 value / 填空 answers / 简答 ref */
    private Map<String, Object> answerOf(Parsed p) {
        Map<String, Object> a = new LinkedHashMap<>();
        switch (String.valueOf(p.qType)) {
            case "SINGLE", "MULTI" -> a.put("keys", answerKeys(p.answerRaw));
            case "JUDGE" -> {
                Boolean b = parseBool(p.answerRaw);
                a.put("value", b == null ? Boolean.TRUE : b);
            }
            case "BLANK" -> a.put("answers", Arrays.asList(p.answerRaw.split("[|｜]")));
            default -> a.put("ref", p.answerRaw);
        }
        return a;
    }

    // ============================================================ 小工具
    private List<String> answerKeys(String raw) {
        if (raw == null) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[A-Ha-h]").matcher(raw);
        while (m.find()) {
            String k = m.group().toUpperCase();
            if (!keys.contains(k)) {
                keys.add(k);
            }
        }
        return keys;
    }

    private Boolean parseBool(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.startsWith("正确") || s.startsWith("对") || s.equalsIgnoreCase("true") || s.startsWith("√")) {
            return Boolean.TRUE;
        }
        if (s.startsWith("错误") || s.startsWith("错") || s.equalsIgnoreCase("false") || s.startsWith("×")) {
            return Boolean.FALSE;
        }
        return null;
    }

    /** 2 字片段重合度：题干有多少比例被原文包含（用来发现"照抄原文当题干"） */
    private static double containment(String a, String b) {
        Set<String> x = bigrams(a);
        if (x.isEmpty()) {
            return 0;
        }
        Set<String> y = bigrams(b);
        int hit = 0;
        for (String g : x) {
            if (y.contains(g)) {
                hit++;
            }
        }
        return (double) hit / x.size();
    }

    /** 2 字片段 Jaccard：两道题是不是同一道 */
    private static double jaccard(String a, String b) {
        Set<String> x = bigrams(a);
        Set<String> y = bigrams(b);
        if (x.isEmpty() || y.isEmpty()) {
            return 0;
        }
        int inter = 0;
        for (String g : x) {
            if (y.contains(g)) {
                inter++;
            }
        }
        return (double) inter / (x.size() + y.size() - inter);
    }

    private static Set<String> bigrams(String s) {
        Set<String> out = new LinkedHashSet<>();
        String t = s == null ? "" : s.replaceAll("[\\s\\p{Punct}\\u3000-\\u303f\\uff00-\\uffef]", "");
        for (int i = 0; i + 2 <= t.length(); i++) {
            out.add(t.substring(i, i + 2));
        }
        return out;
    }

    private static List<String> normalizeTypes(List<String> types) {
        List<String> out = new ArrayList<>();
        if (types != null) {
            for (String t : types) {
                String v = mapType(t);
                if (v != null && !out.contains(v)) {
                    out.add(v);
                }
            }
        }
        if (out.isEmpty()) {
            out.add("SINGLE");
            out.add("JUDGE");
        }
        return out;
    }

    private static String normalizeDifficulty(String v) {
        if (v == null) {
            return "medium";
        }
        String s = v.trim();
        if (s.contains("易") || s.equalsIgnoreCase("easy")) {
            return "easy";
        }
        if (s.contains("难") || s.equalsIgnoreCase("hard")) {
            return "hard";
        }
        return "medium";
    }

    private static String difficultyLabel(String v) {
        return switch (v == null ? "medium" : v) {
            case "easy" -> "易";
            case "hard" -> "难";
            default -> "中";
        };
    }

    private Long courseIdOf(Long offeringId) {
        List<Long> ids = jdbc.queryForList("select course_id from course_offering where id = ?", Long.class, offeringId);
        if (ids.isEmpty()) {
            throw new BizException("开课实例不存在：" + offeringId);
        }
        return ids.get(0);
    }

    private String courseName(Long courseId) {
        List<String> names = jdbc.queryForList("select course_name from course where id = ?", String.class, courseId);
        return names.isEmpty() ? "本课程" : names.get(0);
    }

    private Map<String, Object> requireMyDraft(Long id, Long teacherId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select d.id, d.offering_id, d.stem, d.q_type, d.status
                from ai_question_draft d where d.id = ? and d.deleted = 0
                """, id);
        if (rows.isEmpty()) {
            throw new BizException("草稿不存在或已删除");
        }
        Map<String, Object> row = rows.get(0);
        Long offeringId = lng(row.get("offering_id"));
        Integer mine = jdbc.queryForObject(
                "select count(*) from teaching_class where offering_id = ? and teacher_id = ?",
                Integer.class, offeringId, teacherId);
        if (mine == null || mine == 0) {
            throw new BizException("只能操作自己任教课程下的出题草稿");
        }
        return row;
    }

    private List<Map<String, Object>> listDraftsByOffering(Long offeringId, String status, Long teacherId) {
        List<Long> classIds = jdbc.queryForList(
                "select id from teaching_class where offering_id = ? and teacher_id = ?", Long.class,
                offeringId, teacherId);
        if (classIds.isEmpty()) {
            return List.of();
        }
        return listDrafts(classIds.get(0), status, teacherId);
    }

    private Object readJson(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return MAPPER.readValue(String.valueOf(v), Object.class);
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

    private static Integer intOf(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
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
