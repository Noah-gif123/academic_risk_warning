import com.example.academic_risk_warning.util.JsonObjects;
import com.example.academic_risk_warning.util.RiskMath;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * v3.0 C1/C2 判定引擎消融实验（P0-7）。
 *
 * <p>完全离线：只读 risk_state / risk_engine_config，不写库、不改配置。
 * 与 v2.0 的 {@code AblationTool} 相比：数据库可指定（v2 修前 vs v3 修后），
 * 输出 JSON（含等级变化率、平均绝对风险分变化、因子均值）供证据卡直接引用。</p>
 *
 * <p>用法：{@code java -cp "<backend/build-verify>;<classes>" AblationToolV3
 * --db=study_warning_system_v3 --term=5 --label=修后 --out=evidence/ablation_v3.json}</p>
 */
public class AblationToolV3 {

    private static final BigDecimal ONE = BigDecimal.ONE;

    public static void main(String[] args) throws Exception {
        String db = "study_warning_system_v3";
        long termId = 5L;
        String label = "实验";
        String out = null;
        for (String a : args) {
            if (a.startsWith("--db=")) db = a.substring(5);
            else if (a.startsWith("--term=")) termId = Long.parseLong(a.substring(7));
            else if (a.startsWith("--label=")) label = a.substring(8);
            else if (a.startsWith("--out=")) out = a.substring(6);
        }
        String url = "jdbc:mysql://127.0.0.1:3306/" + db
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
                + "&allowPublicKeyRetrieval=true&useSSL=false";
        try (Connection conn = DriverManager.getConnection(url, "root", "root")) {
            BigDecimal[] th = thresholds(conn);
            BigDecimal red = th[0], orange = th[1], yellow = th[2];
            List<Row> rows = load(conn, termId);
            System.out.println("DB=" + db + " term=" + termId + " rows=" + rows.size()
                    + " thresholds=" + red + "/" + orange + "/" + yellow);

            int mismatch = 0;
            List<String> samples = new ArrayList<>();
            for (Row r : rows) {
                String lv = levelOf(r, r.factorAll(), red, orange, yellow);
                if (!lv.equals(r.level)) {
                    mismatch++;
                    if (samples.size() < 3) samples.add("enrollment " + r.enrollmentId + " db=" + r.level + " re=" + lv);
                }
            }
            if (mismatch > 0) {
                System.out.println("[SELF-CHECK FAILED] " + mismatch + " rows" + samples);
                System.exit(2);
            }
            System.out.println("self-check OK: " + rows.size() + " rows");

            Map<String, BigDecimal[]> variants = new LinkedHashMap<>();
            variants.put("baseline", null);
            variants.put("noCourseType", new BigDecimal[]{ONE, null, null, null});
            variants.put("noFailRate", new BigDecimal[]{null, ONE, null, null});
            variants.put("noTeacher", new BigDecimal[]{null, null, ONE, null});
            variants.put("noPersonal", new BigDecimal[]{null, null, null, ONE});
            variants.put("allOff", new BigDecimal[]{ONE, ONE, ONE, ONE});

            List<String> variantJson = new ArrayList<>();
            BigDecimal baseMeanRisk = BigDecimal.ZERO;
            int baseCount = 0;
            for (Row r : rows) {
                BigDecimal risk = riskOf(r, r.factorAll());
                if (risk != null) { baseMeanRisk = baseMeanRisk.add(risk); baseCount++; }
            }
            BigDecimal meanRisk = baseCount > 0
                    ? baseMeanRisk.divide(BigDecimal.valueOf(baseCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            System.out.printf("%-14s %5s %5s %5s %5s %8s %10s %12s%n",
                    "variant", "RED", "ORANGE", "YELLOW", "GREEN", "changed", "changeRate", "meanAbsDelta");
            for (Map.Entry<String, BigDecimal[]> e : variants.entrySet()) {
                Map<String, Integer> dist = new LinkedHashMap<>();
                dist.put("RED", 0); dist.put("ORANGE", 0); dist.put("YELLOW", 0); dist.put("GREEN", 0);
                int changed = 0;
                BigDecimal absDelta = BigDecimal.ZERO;
                BigDecimal sumRisk = BigDecimal.ZERO;
                int n = 0;
                for (Row r : rows) {
                    BigDecimal factor = e.getValue() == null ? r.factorAll() : r.factorWith(e.getValue());
                    String lv = levelOf(r, factor, red, orange, yellow);
                    dist.merge(lv, 1, Integer::sum);
                    if (!lv.equals(r.level)) changed++;
                    BigDecimal riskOff = riskOf(r, factor);
                    BigDecimal riskOn = riskOf(r, r.factorAll());
                    if (riskOn != null && riskOff != null) {
                        absDelta = absDelta.add(riskOff.subtract(riskOn).abs());
                        sumRisk = sumRisk.add(riskOff);
                        n++;
                    }
                }
                BigDecimal meanAbs = n > 0 ? absDelta.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                BigDecimal meanRiskOff = n > 0 ? sumRisk.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                double rate = rows.isEmpty() ? 0 : changed * 100.0 / rows.size();
                double redYellowRate = rows.isEmpty() ? 0
                        : (dist.get("RED") + dist.get("ORANGE") + dist.get("YELLOW")) * 100.0 / rows.size();
                System.out.printf("%-14s %5d %5d %5d %5d %8d %9.2f%% %12s%n",
                        e.getKey(), dist.get("RED"), dist.get("ORANGE"), dist.get("YELLOW"), dist.get("GREEN"),
                        changed, rate, meanAbs.toPlainString());
                variantJson.add("{\"variant\":\"" + e.getKey() + "\",\"dist\":{"
                        + "\"RED\":" + dist.get("RED") + ",\"ORANGE\":" + dist.get("ORANGE")
                        + ",\"YELLOW\":" + dist.get("YELLOW") + ",\"GREEN\":" + dist.get("GREEN") + "},"
                        + "\"changed\":" + changed + ",\"changeRate\":" + String.format(java.util.Locale.ROOT, "%.2f", rate) + ","
                        + "\"meanAbsDelta\":" + meanAbs.toPlainString()
                        + ",\"meanRisk\":" + meanRiskOff.toPlainString()
                        + ",\"redYellowRate\":" + String.format(java.util.Locale.ROOT, "%.2f", redYellowRate) + "}");
            }

            // C2：阈值单变量扫描（-5/-3/0/+3/+5）
            List<String> sweepJson = new ArrayList<>();
            System.out.println("C2 threshold sweep:");
            for (String which : new String[]{"red", "orange", "yellow"}) {
                BigDecimal center = "red".equals(which) ? red : "orange".equals(which) ? orange : yellow;
                for (int delta : new int[]{-5, -3, 0, 3, 5}) {
                    BigDecimal v = center.add(BigDecimal.valueOf(delta));
                    BigDecimal r = "red".equals(which) ? v : red;
                    BigDecimal o = "orange".equals(which) ? v : orange;
                    BigDecimal y = "yellow".equals(which) ? v : yellow;
                    Map<String, Integer> dist = new LinkedHashMap<>();
                    dist.put("RED", 0); dist.put("ORANGE", 0); dist.put("YELLOW", 0); dist.put("GREEN", 0);
                    for (Row row : rows) dist.merge(levelOf(row, row.factorAll(), r, o, y), 1, Integer::sum);
                    System.out.printf("  %-6s %-6s RED=%d ORANGE=%d YELLOW=%d GREEN=%d%n",
                            which, v.toPlainString(), dist.get("RED"), dist.get("ORANGE"), dist.get("YELLOW"), dist.get("GREEN"));
                    sweepJson.add("{\"threshold\":\"" + which + "\",\"value\":" + v.toPlainString()
                            + ",\"RED\":" + dist.get("RED") + ",\"ORANGE\":" + dist.get("ORANGE")
                            + ",\"YELLOW\":" + dist.get("YELLOW") + ",\"GREEN\":" + dist.get("GREEN") + "}");
                }
            }

            String json = "{\"label\":\"" + escape(label) + "\",\"db\":\"" + escape(db) + "\",\"term\":" + termId
                    + ",\"rows\":" + rows.size()
                    + ",\"thresholds\":{\"red\":" + red + ",\"orange\":" + orange + ",\"yellow\":" + yellow + "}"
                    + ",\"selfCheck\":\"pass\""
                    + ",\"meanRisk\":" + meanRisk.toPlainString()
                    + ",\"variants\":[" + String.join(",", variantJson) + "]"
                    + ",\"thresholdSweep\":[" + String.join(",", sweepJson) + "]}";
            if (out != null) {
                Path p = Path.of(out);
                if (p.getParent() != null) Files.createDirectories(p.getParent());
                Files.writeString(p, json, StandardCharsets.UTF_8);
                System.out.println("written: " + out);
            }
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static BigDecimal riskOf(Row r, BigDecimal factor) {
        if (r.base == null) {
            return null;
        }
        BigDecimal scorePart = RiskMath.clamp100(r.base.multiply(factor).setScale(2, RoundingMode.HALF_UP));
        BigDecimal floor = RiskMath.guardFloor(r.hardFail, r.unreachable, r.remaining);
        return RiskMath.clamp100(scorePart.max(floor));
    }

    private static String levelOf(Row r, BigDecimal factor, BigDecimal red, BigDecimal orange, BigDecimal yellow) {
        BigDecimal risk = riskOf(r, factor);
        if (risk == null) risk = BigDecimal.ZERO;
        return RiskMath.riskLevel(risk, red, orange, yellow);
    }

    private record Row(long enrollmentId, BigDecimal base, boolean hardFail, boolean unreachable,
                       Integer remaining, String level, BigDecimal courseTypeFactor, BigDecimal failRateFactor,
                       BigDecimal teacherFactor, BigDecimal personalFactor) {

        BigDecimal factorAll() {
            return courseTypeFactor.multiply(failRateFactor).multiply(teacherFactor).multiply(personalFactor)
                    .setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal factorWith(BigDecimal[] o) {
            BigDecimal ct = o[0] == null ? courseTypeFactor : o[0];
            BigDecimal fr = o[1] == null ? failRateFactor : o[1];
            BigDecimal te = o[2] == null ? teacherFactor : o[2];
            BigDecimal pe = o[3] == null ? personalFactor : o[3];
            return ct.multiply(fr).multiply(te).multiply(pe).setScale(4, RoundingMode.HALF_UP);
        }
    }

    private static BigDecimal[] thresholds(Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "select threshold_red, threshold_orange, threshold_yellow from risk_engine_config "
                        + "where is_active = 1 order by id desc limit 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new BigDecimal[]{rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getBigDecimal(3)};
            }
        }
        return new BigDecimal[]{BigDecimal.valueOf(75), BigDecimal.valueOf(60), BigDecimal.valueOf(40)};
    }

    private static List<Row> load(Connection conn, long termId) throws Exception {
        List<Row> out = new ArrayList<>();
        String sql = "select enrollment_id, channel_b_score, hard_fail, final_unreachable, remaining_absent,"
                + " risk_level, data_quality_json from risk_state"
                + " where term_id = ? and is_void = 0 and risk_level is not null order by enrollment_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, termId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String json = rs.getString(7);
                    out.add(new Row(rs.getLong(1), rs.getBigDecimal(2), rs.getInt(3) == 1, rs.getInt(4) == 1,
                            rs.getObject(5) == null ? null : rs.getInt(5), rs.getString(6),
                            num(json, "courseTypeFactor"), num(json, "failRateFactor"),
                            num(json, "teacherFactor"), num(json, "personalFactor")));
                }
            }
        }
        return out;
    }

    private static BigDecimal num(String json, String key) {
        if (json == null) return ONE;
        try {
            BigDecimal v = JsonObjects.number(json, key);
            return v == null ? ONE : v;
        } catch (RuntimeException e) {
            return ONE;
        }
    }
}
