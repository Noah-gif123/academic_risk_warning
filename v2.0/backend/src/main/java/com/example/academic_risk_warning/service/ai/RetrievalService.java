package com.example.academic_risk_warning.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 课程资料检索层（R2）：一次检索可以同时走两条路，再融合。
 *
 * <pre>
 *   全文路  MySQL ngram FULLTEXT（精确词、章节号、公式名强）      ← 本地，永不缺席
 *   向量路  Dify 知识库（Weaviate，语义相近能召回）              ← 可选，没配 key 就跳过
 *        └── RRF（Reciprocal Rank Fusion）只看名次不看分数 → 融合榜
 * </pre>
 *
 * <p>三种模式由 {@code app.ai.retrieval.mode} 决定：</p>
 * <ul>
 *   <li>{@code FULLTEXT} —— 只用全文路（向量未配置时的默认行为，与改造前一致）</li>
 *   <li>{@code VECTOR}   —— 只用向量路</li>
 *   <li>{@code HYBRID}   —— 双路 + RRF 融合（论文里的目标形态）</li>
 * </ul>
 *
 * <p>设计原则：向量路**任何失败都不影响全文路**——异常被吞进诊断信息，
 * 结果仍返回全文榜，{@code degraded} 里写明原因，便于在 {@code ai_qa_log.retrieval_json} 里追溯。</p>
 */
@Slf4j
@Service
public class RetrievalService {

    /** RRF 常数：经验值 60，越大越"平权"，越小越偏向单路头部 */
    private static final int RRF_K = 60;

    private final JdbcTemplate jdbc;
    private final DifyVectorClient vectorClient;
    private final String mode;
    private final int poolSize;
    /** 单次结果里同一份文档最多占几个名额（抑制同源分块占满 top-k） */
    private final int maxPerDoc;
    /** 词面支撑度下限（全文路护栏；做成字段是为了能扫参做实验） */
    private final double minLexSupport;

    public RetrievalService(JdbcTemplate jdbc,
                            DifyVectorClient vectorClient,
                            @Value("${ai.retrieval.mode:HYBRID}") String mode,
                            @Value("${ai.retrieval.pool-size:20}") int poolSize,
                            @Value("${ai.retrieval.max-per-doc:2}") int maxPerDoc,
                            @Value("${ai.retrieval.min-lex-support:0.10}") double minLexSupport) {
        this.jdbc = jdbc;
        this.vectorClient = vectorClient;
        this.mode = mode == null ? "HYBRID" : mode.trim().toUpperCase();
        this.poolSize = Math.max(4, Math.min(poolSize, 50));
        this.maxPerDoc = Math.max(1, Math.min(maxPerDoc, 10));
        this.minLexSupport = minLexSupport <= 0 ? 0.10 : minLexSupport;
        log.info("[RAG] 检索配置：mode={} poolSize={} maxPerDoc={} minLexSupport={} vectorConfigured={}",
                this.mode, this.poolSize, this.maxPerDoc, this.minLexSupport, vectorClient.configured());
    }

    /** 用另一组参数克隆一份（评测扫参用，避免为一个参数重启服务） */
    public RetrievalService copyWith(double minLexSupport, int maxPerDoc) {
        return new RetrievalService(jdbc, vectorClient, mode, poolSize,
                maxPerDoc > 0 ? maxPerDoc : this.maxPerDoc,
                minLexSupport > 0 ? minLexSupport : this.minLexSupport);
    }

    public enum Mode { FULLTEXT, VECTOR, HYBRID }

    /** 一条候选；两条路的原始名次都留着，方便做消融对比 */
    public record Hit(Long chunkId, String docName, Integer sectionNo, String sourceType, String content,
                      Double ftScore, Integer ftRank, Double vecScore, Integer vecRank,
                      double fused, int rank) {
        boolean contains(String q) {
            return content != null && content.contains(q);
        }
    }

    /** 一次检索的结果 + 诊断 */
    public record Outcome(Mode mode, Mode effective, List<Hit> hits, double fulltextTop, double vectorTop,
                          int fulltextCount, int vectorCount, long costMs, String degraded) {
    }

    // ================================================================== 对外入口

    /** 按配置的模式检索 */
    public Outcome retrieve(Long courseId, String query, int topK) {
        return retrieve(courseId, query, topK, parseMode(mode));
    }

    /** 指定模式检索（评测脚本用它跑 全文 / 向量 / 混合 三行对比） */
    public Outcome retrieve(Long courseId, String query, int topK, Mode want) {
        long t0 = System.currentTimeMillis();
        int k = Math.max(1, Math.min(topK, 10));
        String q = normalize(query);
        if (q.isBlank()) {
            return new Outcome(want, want, List.of(), 0, 0, 0, 0, 0, "查询为空");
        }

        List<Hit> ft = want == Mode.VECTOR ? List.of() : fulltext(courseId, q);
        List<Hit> vec = List.of();
        String degraded = null;
        if (want != Mode.FULLTEXT) {
            if (!vectorClient.configured()) {
                degraded = vectorClient.notConfiguredReason();
            } else {
                try {
                    vec = vectorToHits(vectorClient.search(courseId, q));
                } catch (Exception e) {
                    degraded = "向量通道失败：" + e.getMessage();
                    log.warn("[RAG] 向量路失败，降级为全文路：{}", e.getMessage());
                }
            }
        }

        Mode effective = want;
        if (want == Mode.HYBRID && vec.isEmpty()) {
            effective = Mode.FULLTEXT;
        } else if (want == Mode.VECTOR && vec.isEmpty()) {
            effective = Mode.FULLTEXT;   // 向量路没结果时兜底全文，避免"什么都没检索到"
            ft = fulltext(courseId, q);
        }

        List<Hit> result;
        if (effective == Mode.HYBRID) {
            result = rrf(ft, vec, k);
        } else if (effective == Mode.VECTOR) {
            result = topOf(vec, k);
        } else {
            result = topOf(ft, k);
        }
        result = lightweightRerank(result, q, k);

        double ftTop = ft.isEmpty() || ft.get(0).ftScore() == null ? 0d : ft.get(0).ftScore();
        double vecTop = vec.isEmpty() ? 0d : vec.get(0).vecScore() == null ? 0d : vec.get(0).vecScore();
        return new Outcome(want, effective, result, ftTop, vecTop, ft.size(), vec.size(),
                System.currentTimeMillis() - t0, degraded);
    }

    /** 兼容旧接口：返回 {chunkId, docName, sectionNo, sourceType, score, snippet, pageNo, content} */
    public List<Map<String, Object>> retrieveAsMaps(Long courseId, String query, int topK) {
        return toMaps(retrieve(courseId, query, topK).hits());
    }

    public static List<Map<String, Object>> toMaps(List<Hit> hits) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Hit h : hits) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("chunkId", h.chunkId());
            m.put("docName", h.docName());
            m.put("sectionNo", h.sectionNo());
            m.put("sourceType", h.sourceType());
            // score 口径保持与改造前一致：优先全文分（MIN_SCORE 是照着它的量级定的），没有全文分才用向量分
            m.put("score", h.ftScore() != null ? h.ftScore() : h.vecScore());
            String content = h.content() == null ? "" : h.content();
            m.put("pageNo", KbExtractor.pageOf(content));
            m.put("snippet", content.length() <= 220 ? content : content.substring(0, 219) + "…");
            m.put("content", content);
            m.put("fusedScore", h.fused());
            m.put("path", h.ftRank() != null && h.vecRank() != null ? "BOTH"
                    : h.ftRank() != null ? "FULLTEXT" : "VECTOR");
            out.add(m);
        }
        return out;
    }

    // ================================================================== 全文路

    private List<Hit> fulltext(Long courseId, String q) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try {
            rows = jdbc.queryForList("""
                    select id, doc_name, section_no, source_type, content,
                           match(content) against (? in natural language mode) as score
                    from ai_kb_chunk
                    where course_id = ? and match(content) against (? in natural language mode)
                    order by score desc
                    limit ?
                    """, q, courseId, q, poolSize);
        } catch (Exception e) {
            log.warn("[RAG] 全文检索失败，回退 LIKE：{}", e.getMessage());
        }
        if (rows.isEmpty()) {
            for (String t : terms(q)) {
                rows = jdbc.queryForList("""
                        select id, doc_name, section_no, source_type, content, 0.05 as score
                        from ai_kb_chunk where course_id = ? and content like ? limit ?
                        """, courseId, "%" + t + "%", poolSize);
                if (!rows.isEmpty()) {
                    break;
                }
            }
        }
        List<Hit> out = new ArrayList<>();
        int rank = 0;
        for (Map<String, Object> r : rows) {
            rank++;
            double s = r.get("score") == null ? 0d : Double.parseDouble(String.valueOf(r.get("score")));
            out.add(new Hit(asLong(r.get("id")), str(r.get("doc_name")), asInt(r.get("section_no")),
                    str(r.get("source_type")), str(r.get("content")), s, rank, null, null, 0d, rank));
        }
        return out;
    }

    // ================================================================== 向量路

    /** Dify segment → 统一候选。优先按内容与本地块对齐，对不上就保留 Dify 的东西（chunkId 为空） */
    private List<Hit> vectorToHits(List<DifyVectorClient.VecHit> vec) {
        List<Hit> out = new ArrayList<>();
        for (DifyVectorClient.VecHit v : vec) {
            Long chunkId = null;
            Integer sectionNo = v.position();
            String docName = v.docName() == null || v.docName().isBlank() ? "Dify 知识库资料" : v.docName();
            String content = v.content() == null ? "" : v.content();
            List<Map<String, Object>> local = jdbc.queryForList("""
                    select id, section_no from ai_kb_chunk
                    where doc_name = ? and (content = ? or left(content, 80) = left(?, 80))
                    limit 1
                    """, docName, content, content);
            if (!local.isEmpty()) {
                chunkId = asLong(local.get(0).get("id"));
                sectionNo = asInt(local.get(0).get("section_no"));
            }
            out.add(new Hit(chunkId, docName, sectionNo, "VECTOR", content,
                    null, null, v.score(), v.rank(), 0d, v.rank()));
        }
        return out;
    }

    // ================================================================== 融合

    /** 轻量 rerank：在 RRF 名次基础上，用查询词面覆盖度做二次排序（不引入外部模型）。 */
    private List<Hit> lightweightRerank(List<Hit> in, String query, int topK) {
        if (in.size() <= 1) return in;
        String q = query == null ? "" : query;
        List<Hit> scored = new ArrayList<>();
        for (Hit h : in) {
            String content = h.content() == null ? "" : h.content();
            int hit = 0;
            for (int i = 0; i + 2 <= q.length(); i++) {
                if (content.contains(q.substring(i, i + 2))) hit++;
            }
            double lex = q.length() < 2 ? 0 : Math.min(1.0, hit / (double) Math.max(1, q.length() - 1));
            double fused = h.fused() + lex * 0.02;
            scored.add(new Hit(h.chunkId(), h.docName(), h.sectionNo(), h.sourceType(), h.content(),
                    h.ftScore(), h.ftRank(), h.vecScore(), h.vecRank(), fused, h.rank()));
        }
        scored.sort((a, b) -> Double.compare(b.fused(), a.fused()));
        return scored.stream().limit(topK).toList();
    }

    /** RRF：fused = Σ 1/(K + rank)，两路都命中的条目自然排前 */
    private List<Hit> rrf(List<Hit> ft, List<Hit> vec, int topK) {
        Map<String, Hit> merged = new LinkedHashMap<>();
        Map<String, Double> score = new LinkedHashMap<>();
        for (Hit h : ft) {
            String key = keyOf(h);
            merged.put(key, h);
            score.merge(key, 1.0 / (RRF_K + h.ftRank()), Double::sum);
        }
        for (Hit h : vec) {
            String key = keyOf(h);
            Hit exist = merged.get(key);
            if (exist == null) {
                merged.put(key, h);
            } else {
                merged.put(key, new Hit(exist.chunkId(), exist.docName(), exist.sectionNo(), exist.sourceType(),
                        exist.content(), exist.ftScore(), exist.ftRank(), h.vecScore(), h.vecRank(), 0d, 0));
            }
            score.merge(key, 1.0 / (RRF_K + h.vecRank()), Double::sum);
        }
        List<Hit> out = new ArrayList<>();
        for (Map.Entry<String, Hit> e : merged.entrySet()) {
            Hit h = e.getValue();
            double f = score.getOrDefault(e.getKey(), 0d);
            out.add(new Hit(h.chunkId(), h.docName(), h.sectionNo(), h.sourceType(), h.content(),
                    h.ftScore(), h.ftRank(), h.vecScore(), h.vecRank(), f, 0));
        }
        out.sort(Comparator.comparingDouble(Hit::fused).reversed()
                .thenComparing(h -> h.ftRank() == null ? Integer.MAX_VALUE : h.ftRank()));
        List<Hit> ranked = new ArrayList<>();
        for (Hit h : diversify(out, topK)) {
            ranked.add(new Hit(h.chunkId(), h.docName(), h.sectionNo(), h.sourceType(), h.content(),
                    h.ftScore(), h.ftRank(), h.vecScore(), h.vecRank(), h.fused(), ranked.size() + 1));
        }
        return ranked;
    }

    private List<Hit> topOf(List<Hit> hits, int topK) {
        List<Hit> ranked = new ArrayList<>();
        for (Hit h : diversify(hits, topK)) {
            double f = h.ftScore() != null ? h.ftScore() : (h.vecScore() == null ? 0d : h.vecScore());
            ranked.add(new Hit(h.chunkId(), h.docName(), h.sectionNo(), h.sourceType(), h.content(),
                    h.ftScore(), h.ftRank(), h.vecScore(), h.vecRank(), f, ranked.size() + 1));
        }
        return ranked;
    }

    /**
     * 同源抑制：按分数顺序挑，但同一份文档最多占 {@code maxPerDoc} 个名额。
     *
     * <p>语料只有 40 块时，一条命中就代表一份文档；扩到 200+ 块之后，同一份课件/习题集会有十几个分块，
     * 分数接近，很容易把 top-4 全占满，导致"别的文档压根没机会出现"——实测 recall@4 从 0.97 掉到 0.78
     * 主要就是这个原因，不是检索不到，而是选不出。</p>
     */
    private List<Hit> diversify(List<Hit> hits, int topK) {
        List<Hit> picked = new ArrayList<>();
        Map<String, Integer> perDoc = new LinkedHashMap<>();
        List<Hit> overflow = new ArrayList<>();
        for (Hit h : hits) {
            String doc = h.docName() == null ? "" : h.docName();
            int used = perDoc.getOrDefault(doc, 0);
            if (used < maxPerDoc) {
                picked.add(h);
                perDoc.put(doc, used + 1);
                if (picked.size() >= topK) {
                    return picked;
                }
            } else {
                overflow.add(h);
            }
        }
        // 名额没用满（候选文档不够多）就按原顺序补回来，宁可同源也不留空
        for (Hit h : overflow) {
            if (picked.size() >= topK) {
                break;
            }
            picked.add(h);
        }
        return picked;
    }

    /** 同一条资料在两条路里的身份：优先本地块 ID，其次「文档名#段号」，最后用内容前缀 */
    private static String keyOf(Hit h) {
        if (h.chunkId() != null) {
            return "C" + h.chunkId();
        }
        if (h.docName() != null && h.sectionNo() != null) {
            return "D" + h.docName() + "#" + h.sectionNo();
        }
        String c = h.content() == null ? "" : h.content();
        return "T" + c.substring(0, Math.min(40, c.length()));
    }

    // ================================================================== 诊断

    /** 写进 ai_qa_log.retrieval_json 的诊断包 */
    public Map<String, Object> diagnostics(Outcome o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("want", o.mode().name());
        m.put("effective", o.effective().name());
        m.put("fulltextHits", o.fulltextCount());
        m.put("vectorHits", o.vectorCount());
        m.put("fulltextTop", round(o.fulltextTop()));
        m.put("vectorTop", round(o.vectorTop()));
        m.put("costMs", o.costMs());
        if (o.degraded() != null) {
            m.put("degraded", o.degraded());
        }
        List<Map<String, Object>> rank = new ArrayList<>();
        for (Hit h : o.hits()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("docName", h.docName());
            r.put("sectionNo", h.sectionNo());
            r.put("rank", h.rank());
            r.put("ftRank", h.ftRank());
            r.put("vecRank", h.vecRank());
            r.put("fused", round(h.fused()));
            rank.add(r);
        }
        m.put("top", rank);
        return m;
    }

    // ================================================================== 拒答判据

    /** 全文相关度下限（ngram 分值随语料规模变化，取绝对下限） */
    private static final double MIN_SCORE = 0.10;

    /** 词面支撑度下限：只用于全文路——语义召回天然字面重合低，不能套这个口径 */
    private static final double MIN_LEX_SUPPORT = 0.10;

    /** 纯向量命中的相似度下限（Dify 召回分是归一化相似度，与 ngram 分值不同量级） */
    private static final double MIN_VECTOR_SCORE = 0.35;

    /** 拒答判定结果：给线上问答与评测脚本共用，保证"评测测的就是线上行为" */
    public record Guard(boolean refuse, String reason, double topScore, double lexSupport) {
    }

    /**
     * 有没有足够依据回答这个问题。
     *
     * <p>规则按命中来源分流：全文命中看「相关度 + 词面支撑」；纯向量命中只看「向量相似度」，
     * 因为"平时分怎么算"和"课程成绩构成比例"字面几乎不重合，却完全相关——拿词面支撑度卡它必然误拒。</p>
     */
    public Guard guard(Outcome o, String question) {
        if (o.hits().isEmpty()) {
            return new Guard(true, "无检索结果", 0d, 0d);
        }
        Hit top = o.hits().get(0);
        // 词面支撑度取 top-k 里的最大值：只看第一名会被"表格碎片 / 分段边界"误伤
        // （实例：问"总评成绩由哪几部分构成"，命中的是表 3 碎片，单看它只有 0.176，
        //  但同一批命中里导学那段覆盖了全部关键词，按 top-1 判会把这个核心问题误拒）
        double lex = 0d;
        for (Hit h : o.hits()) {
            lex = Math.max(lex, lexicalSupport(question, h.content()));
        }
        double topScore = top.ftScore() != null ? top.ftScore()
                : (top.vecScore() == null ? 0d : top.vecScore());
        if (top.ftRank() != null) {
            if (topScore < MIN_SCORE) {
                return new Guard(true, "全文相关度低于阈值 " + MIN_SCORE, topScore, lex);
            }
            if (lex < minLexSupport) {
                return new Guard(true, "词面支撑度低于阈值 " + minLexSupport, topScore, lex);
            }
            return new Guard(false, null, topScore, lex);
        }
        if (topScore < MIN_VECTOR_SCORE) {
            return new Guard(true, "向量相似度低于阈值 " + MIN_VECTOR_SCORE, topScore, lex);
        }
        return new Guard(false, null, topScore, lex);
    }

    /** 词面支撑度：问题切成 2 字片段，统计有多少比例出现在片段正文里（与语料规模无关） */
    public static double lexicalSupport(String question, String content) {
        if (question == null || content == null) {
            return 0d;
        }
        String q = question.replaceAll("[\\s\\p{Punct}\\u3000-\\u303f\\uff00-\\uffef]", "").toLowerCase();
        String c = content.toLowerCase();
        if (q.length() < 2) {
            return 0d;
        }
        Set<String> grams = new LinkedHashSet<>();
        for (int i = 0; i + 2 <= q.length(); i++) {
            grams.add(q.substring(i, i + 2));
        }
        int hit = 0;
        for (String g : grams) {
            if (c.contains(g)) {
                hit++;
            }
        }
        return (double) hit / grams.size();
    }

    // ================================================================== 小工具

    public static Mode parseMode(String s) {
        if (s == null) {
            return Mode.HYBRID;
        }
        try {
            return Mode.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            return Mode.HYBRID;
        }
    }

    private static double round(double d) {
        return Math.round(d * 10000d) / 10000d;
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\r', '\n').replaceAll("[ \\t\\u00a0]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }

    private static List<String> terms(String q) {
        Set<String> out = new LinkedHashSet<>();
        for (String t : q.split("[\\s,，。；;、:：/\\\\()（）\\[\\]\"'“”‘’]+")) {
            if (t.length() >= 2) {
                out.add(t);
            }
        }
        if (out.isEmpty() && q.length() >= 2) {
            out.add(q);
        }
        return out.size() > 4 ? new ArrayList<>(out).subList(0, 4) : new ArrayList<>(out);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Long asLong(Object o) {
        try {
            return o == null ? null : Long.parseLong(String.valueOf(o).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer asInt(Object o) {
        try {
            return o == null ? null : (int) Math.round(Double.parseDouble(String.valueOf(o)));
        } catch (Exception e) {
            return null;
        }
    }
}
