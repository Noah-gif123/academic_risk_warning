package com.example.academic_risk_warning.agent.ledger;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * P2-6 EvidenceLedger：入账 / 校验 / 一次重写 / 降级，落库 ai_evidence_ledger。
 *
 * <p>规则：工具返回的每个标量证据（数字/字符串/布尔）入账；Writer 答案中的每个数字
 * 必须能在台账中找到（数值相等或文本包含），否则未命中；未命中先重写一次，
 * 仍不通过则标记 degraded。</p>
 */
@Service
@RequiredArgsConstructor
public class EvidenceLedger {

    private static final Pattern NUMBER = Pattern.compile("(?<![A-Za-z_])\\d+(?:\\.\\d+)?");

    private final JdbcTemplate jdbc;

    public record Entry(int stepNo, String tool, String metric, String valueText, BigDecimal numeric, String source) {
    }

    public record Validation(String runKey, int entries, List<String> numbers, List<String> hits,
                             List<String> misses, double hitRate) {
    }

    public record Repair(Validation first, boolean rewritten, String rewrittenAnswer,
                         Validation second, boolean degraded, String finalAnswer) {
    }

    public String newRunKey() {
        return "run-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    /** 入账：只收标量证据，列表/对象跳过（其数量已由工具放入 evidence）。 */
    public int record(String runKey, int stepNo, String tool, Map<String, Object> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return 0;
        }
        String source = evidence.get("source") == null ? null : String.valueOf(evidence.get("source"));
        int n = 0;
        for (Map.Entry<String, Object> e : evidence.entrySet()) {
            Object v = e.getValue();
            if (v == null || v instanceof Map || v instanceof List) {
                continue;
            }
            String text = String.valueOf(v);
            BigDecimal numeric = null;
            if (v instanceof Number) {
                numeric = new BigDecimal(String.valueOf(v));
            }
            jdbc.update("insert into ai_evidence_ledger (run_key, step_no, tool_name, metric, value_text, numeric_value, source)"
                            + " values (?,?,?,?,?,?,?)",
                    runKey, stepNo, tool, e.getKey(), text, numeric, source);
            n++;
        }
        return n;
    }

    public List<Entry> entries(String runKey) {
        return jdbc.query("select step_no, tool_name, metric, value_text, numeric_value, source"
                        + " from ai_evidence_ledger where run_key=? order by id",
                (rs, i) -> new Entry(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getBigDecimal(5), rs.getString(6)), runKey);
    }

    /** 校验答案中的每个数字是否命中台账。 */
    public Validation validate(String runKey, String answer) {
        List<Entry> entries = entries(runKey);
        List<String> numbers = new ArrayList<>();
        List<String> hits = new ArrayList<>();
        List<String> misses = new ArrayList<>();
        if (answer != null) {
            Matcher m = NUMBER.matcher(answer);
            while (m.find()) {
                numbers.add(m.group());
            }
        }
        Set<String> seenNumbers = new LinkedHashSet<>(numbers);
        for (String token : seenNumbers) {
            boolean hit = false;
            BigDecimal tokenNum = null;
            try {
                tokenNum = new BigDecimal(token);
            } catch (Exception ignored) {
            }
            for (Entry e : entries) {
                if (tokenNum != null && e.numeric() != null && e.numeric().compareTo(tokenNum) == 0) {
                    hit = true;
                    break;
                }
                if (e.valueText() != null && e.valueText().contains(token)) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                hits.add(token);
            } else {
                misses.add(token);
            }
        }
        double rate = numbers.isEmpty() ? 1.0 : ((double) hits.size()) / seenNumbers.size();
        return new Validation(runKey, entries.size(), numbers, hits, misses, round(rate));
    }

    /**
     * 一次重写/降级：未命中 → 用台账重建答案；台账为空则直接降级；
     * 重写后仍未命中 → 降级。
     */
    public Repair repair(String runKey, String answer) {
        Validation first = validate(runKey, answer);
        if (first.misses().isEmpty()) {
            return new Repair(first, false, null, first, false, answer);
        }
        if (first.entries() == 0) {
            String degraded = "部分数字未入账，已降级：" + answer;
            return new Repair(first, false, null, first, true, degraded);
        }
        String rewritten = rewrite(runKey);
        Validation second = validate(runKey, rewritten);
        boolean degraded = !second.misses().isEmpty();
        String finalAnswer = "（已按台账重写）" + rewritten;
        if (degraded) {
            finalAnswer = "部分数字未入账，已降级：" + rewritten;
        }
        return new Repair(first, true, rewritten, second, degraded, finalAnswer);
    }

    /** 一次重写：仅用台账中的值重建答案。 */
    public String rewrite(String runKey) {
        List<Entry> entries = entries(runKey);
        StringBuilder sb = new StringBuilder("根据已入账工具证据：");
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (i > 0) {
                sb.append("，");
            }
            sb.append(e.metric()).append('=').append(e.valueText());
        }
        return sb.toString();
    }

    private double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
