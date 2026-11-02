package com.example.academic_risk_warning.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 检索评测（R4）：把「全文 / 向量 / 混合」三条路放在同一批问题上跑，产出可写进论文的指标。
 *
 * <p>关键设计：<b>判分口径与线上一致</b>——是否拒答直接调 {@link RetrievalService#guard}，
 * 而不是评测脚本自己另立一套阈值，否则"评测指标好"和"线上表现"会脱节。</p>
 *
 * <p>指标定义：</p>
 * <ul>
 *   <li>{@code recall@k}：可答问题里，标准出处出现在前 k 条命中的比例（文档级）</li>
 *   <li>{@code MRR}：第一条命中正确出处的名次的倒数均值</li>
 *   <li>{@code nDCG@k}：二值相关性下的归一化折损累计增益</li>
 *   <li>{@code refuseAcc}：不可答问题被正确拒答的比例</li>
 *   <li>{@code falseRefuse}：可答问题被误拒的比例（越低越好）</li>
 * </ul>
 */
@Slf4j
@Service
public class RagEvalService {

    private final JdbcTemplate jdbc;
    private final RetrievalService retrievalService;

    public RagEvalService(JdbcTemplate jdbc, RetrievalService retrievalService) {
        this.jdbc = jdbc;
        this.retrievalService = retrievalService;
    }

    /** 一条评测用例 */
    public record Case(long id, long courseId, String question, boolean answerable,
                       List<String> goldDocs, Integer goldSection) {
    }

    /** 跑一种模式，返回指标并落库 */
    public Map<String, Object> run(RetrievalService.Mode mode, int k) {
        int topK = Math.max(1, Math.min(k, 10));
        List<Case> cases = loadCases();
        if (cases.isEmpty()) {
            throw new IllegalStateException("评测集为空：先执行 sql/38_seed_rag_eval.sql");
        }
        double recallSum = 0, mrrSum = 0, ndcgSum = 0;
        int answerable = 0, unanswerable = 0, refuseRight = 0, refuseWrong = 0, hitRight = 0;
        int fallbacks = 0;
        long totalMs = 0;
        List<Map<String, Object>> detail = new ArrayList<>();

        for (Case c : cases) {
            long t0 = System.currentTimeMillis();
            RetrievalService.Outcome o = retrievalService.retrieve(c.courseId(), c.question(), topK, mode);
            RetrievalService.Guard g = retrievalService.guard(o, c.question());
            long ms = System.currentTimeMillis() - t0;
            totalMs += ms;
            if (o.effective() != mode) {
                // 想要的路没跑起来（例如向量通道未配置），实际用的是别的那条路
                fallbacks++;
            }

            Integer firstRank = firstCorrectRank(o.hits(), c.goldDocs());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("caseId", c.id());
            row.put("question", c.question());
            row.put("answerable", c.answerable());
            row.put("mode", o.effective().name());
            row.put("hits", o.hits().size());
            row.put("firstCorrectRank", firstRank);
            row.put("refused", g.refuse());
            row.put("refuseReason", g.reason());
            row.put("topScore", round(g.topScore()));
            row.put("lexSupport", round(g.lexSupport()));
            row.put("costMs", ms);
            row.put("topDocs", o.hits().stream().limit(topK).map(RetrievalService.Hit::docName).toList());
            detail.add(row);

            if (c.answerable()) {
                answerable++;
                if (firstRank != null) {
                    hitRight++;
                    mrrSum += 1.0 / firstRank;
                    ndcgSum += 1.0 / (Math.log(firstRank + 1) / Math.log(2));
                }
                if (g.refuse()) {
                    refuseWrong++;
                }
            } else {
                unanswerable++;
                if (g.refuse()) {
                    refuseRight++;
                }
            }
        }

        double recall = answerable == 0 ? 0 : (double) hitRight / answerable;
        double mrr = answerable == 0 ? 0 : mrrSum / answerable;
        double ndcg = answerable == 0 ? 0 : ndcgSum / answerable;
        double refuseAcc = unanswerable == 0 ? 0 : (double) refuseRight / unanswerable;
        double falseRefuse = answerable == 0 ? 0 : (double) refuseWrong / answerable;
        int avgMs = cases.isEmpty() ? 0 : (int) (totalMs / cases.size());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mode", mode.name());
        out.put("k", topK);
        out.put("cases", cases.size());
        out.put("answerable", answerable);
        out.put("unanswerable", unanswerable);
        out.put("recallAtK", round4(recall));
        out.put("mrr", round4(mrr));
        out.put("ndcgAtK", round4(ndcg));
        out.put("refuseAccuracy", round4(refuseAcc));
        out.put("falseRefuse", round4(falseRefuse));
        out.put("avgMs", avgMs);
        out.put("effectiveFallback", fallbacks);
        if (fallbacks > 0) {
            // 三行对比表里必须写明这一行其实是什么路跑出来的，不能拿"配置未生效"的数字当结论
            out.put("note", fallbacks + "/" + cases.size()
                    + " 条用例并未真正走 " + mode.name() + "（向量通道未配置或失败，已降级），该行指标不代表该模式");
        }

        try {
            jdbc.update("""
                    insert into ai_rag_eval_run(mode, k, cases, recall_at_k, mrr, ndcg_at_k,
                                                refuse_acc, false_refuse, avg_ms, detail_json)
                    values (?,?,?,?,?,?,?,?,?,?)
                    """, mode.name(), topK, cases.size(),
                    BigDecimal.valueOf(recall), BigDecimal.valueOf(mrr), BigDecimal.valueOf(ndcg),
                    BigDecimal.valueOf(refuseAcc), BigDecimal.valueOf(falseRefuse), avgMs,
                    toJson(Map.of("cases", detail, "effectiveFallback", fallbacks)));
        } catch (Exception e) {
            log.warn("[RAG-EVAL] 结果落库失败：{}", e.getMessage());
        }

        log.info("[RAG-EVAL] mode={} k={} recall@k={} mrr={} ndcg={} refuseAcc={} falseRefuse={} avgMs={}",
                mode, topK, round4(recall), round4(mrr), round4(ndcg), round4(refuseAcc), round4(falseRefuse), avgMs);
        out.put("detail", detail);
        return out;
    }

    /** 三行对比：FULLTEXT / VECTOR / HYBRID 各跑一遍 */
    public Map<String, Object> compareAll(int k) {
        Map<String, Object> table = new LinkedHashMap<>();
        for (RetrievalService.Mode m : RetrievalService.Mode.values()) {
            Map<String, Object> r = run(m, k);
            r.remove("detail");
            table.put(m.name(), r);
        }
        return table;
    }

    private List<Case> loadCases() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select id, course_id, question, answerable, gold_doc, gold_section
                from ai_rag_eval_case order by course_id, id
                """);
        List<Case> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String gold = r.get("gold_doc") == null ? null : String.valueOf(r.get("gold_doc"));
            List<String> docs = new ArrayList<>();
            if (gold != null && !gold.isBlank()) {
                // 同一份导学发了多个教学班（CS-01-A / CS-02-A …），内容相同，命中任一都算对
                for (String d : gold.split("\\|")) {
                    if (!d.isBlank()) {
                        docs.add(d.trim());
                    }
                }
            }
            out.add(new Case(
                    ((Number) r.get("id")).longValue(),
                    ((Number) r.get("course_id")).longValue(),
                    String.valueOf(r.get("question")),
                    asBool(r.get("answerable")),
                    docs,
                    r.get("gold_section") == null ? null : ((Number) r.get("gold_section")).intValue()));
        }
        return out;
    }

    /** MySQL 的 tinyint(1) 会被 JDBC 映射成 Boolean，这里两种都认 */
    private static boolean asBool(Object v) {
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Number n) {
            return n.intValue() == 1;
        }
        return "1".equals(String.valueOf(v)) || "true".equalsIgnoreCase(String.valueOf(v));
    }

    /** 第一条命中正确出处的名次（文档级），没命中返回 null */
    private static Integer firstCorrectRank(List<RetrievalService.Hit> hits, List<String> goldDocs) {
        if (goldDocs.isEmpty()) {
            return null;
        }
        for (RetrievalService.Hit h : hits) {
            String doc = h.docName() == null ? "" : h.docName();
            for (String g : goldDocs) {
                if (doc.contains(g) || g.contains(doc) && !doc.isBlank()) {
                    return h.rank();
                }
            }
        }
        return null;
    }

    private static String toJson(Object o) {
        try {
            return new tools.jackson.databind.ObjectMapper().writeValueAsString(o);
        } catch (Exception e) {
            return null;
        }
    }

    private static double round(double d) {
        return Math.round(d * 10000d) / 10000d;
    }

    private static double round4(double d) {
        return Math.round(d * 10000d) / 10000d;
    }
}
