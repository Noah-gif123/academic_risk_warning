import com.example.academic_risk_warning.util.RiskMath;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * C1 判定引擎消融实验：把 C 通道四个因子逐个置 1.0，看有多少学生的预警等级发生变化。
 *
 * <p>完全离线：只读 risk_state 与 risk_engine_config，不写库、不改配置、不重跑引擎。
 * 复算公式与 {@code RiskEngineService.decide()} 保持一致（顺序见类内注释），
 * 因此第一步会先**自检**：用完整因子复算的结果必须与库里的 risk_level 完全一致；
 * 若自检不通过，说明公式复刻有偏差，实验结论不可信，工具会直接报错退出。</p>
 *
 * <p>用法：{@code java -cp ".;<backend/build-verify>;<libs>" AblationTool [termId]}</p>
 */
public class AblationTool {

    private static final String DB = "jdbc:mysql://127.0.0.1:3306/study_warning_system_v2"
            + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final BigDecimal ONE = BigDecimal.ONE;

    public static void main(String[] args) throws Exception {
        long termId = args.length > 0 ? Long.parseLong(args[0]) : 5L;
        try (Connection conn = DriverManager.getConnection(DB, "root", "root")) {
            BigDecimal[] th = thresholds(conn);
            BigDecimal red = th[0], orange = th[1], yellow = th[2];
            System.out.printf("阈值：红≥%s 橙≥%s 黄≥%s%n", red, orange, yellow);

            List<Row> rows = load(conn, termId);
            System.out.println("term " + termId + " 参与消融的判定行：" + rows.size());

            // ---- 自检：完整因子复算必须与库内结果一致
            int mismatch = 0;
            List<String> samples = new ArrayList<>();
            for (Row r : rows) {
                String recomputed = levelOf(r, r.factorAll(), red, orange, yellow);
                if (!recomputed.equals(r.level)) {
                    mismatch++;
                    if (samples.size() < 3) {
                        samples.add("enrollment " + r.enrollmentId + " 库内=" + r.level + " 复算=" + recomputed
                                + " base=" + r.base + " factor=" + r.factorAll());
                    }
                }
            }
            if (mismatch > 0) {
                System.out.println("\n[自检失败] " + mismatch + " 行复算不一致，示例：");
                samples.forEach(s -> System.out.println("  " + s));
                System.out.println("结论：公式复刻与实际引擎不一致，消融结果不可用，请先修正工具。");
                return;
            }
            System.out.println("自检通过：全部 " + rows.size() + " 行复算等级与库内一致\n");

            if (args.length > 1 && "sweep".equals(args[1])) {
                sweep(rows, red, orange, yellow);
                return;
            }
            Map<String, BigDecimal[]> variants = new LinkedHashMap<>();
            variants.put("基线（全因子）", null);
            variants.put("关闭 课程类型因子", new BigDecimal[]{ONE, null, null, null});
            variants.put("关闭 历史挂科率因子", new BigDecimal[]{null, ONE, null, null});
            variants.put("关闭 教师风格因子", new BigDecimal[]{null, null, ONE, null});
            variants.put("关闭 个人先验因子", new BigDecimal[]{null, null, null, ONE});
            variants.put("全部关闭（退化 A/B）", new BigDecimal[]{ONE, ONE, ONE, ONE});

            System.out.printf("%-22s %6s %6s %6s %6s %8s %10s%n",
                    "实验", "红", "橙", "黄", "绿", "等级变化", "变化率");
            for (Map.Entry<String, BigDecimal[]> e : variants.entrySet()) {
                Map<String, Integer> dist = new LinkedHashMap<>();
                dist.put("RED", 0);
                dist.put("ORANGE", 0);
                dist.put("YELLOW", 0);
                dist.put("GREEN", 0);
                int changed = 0;
                for (Row r : rows) {
                    BigDecimal factor = e.getValue() == null ? r.factorAll() : r.factorWith(e.getValue());
                    String lv = levelOf(r, factor, red, orange, yellow);
                    dist.merge(lv, 1, Integer::sum);
                    if (!lv.equals(r.level)) {
                        changed++;
                    }
                }
                System.out.printf("%-22s %6d %6d %6d %6d %8d %9.1f%%%n",
                        e.getKey(), dist.get("RED"), dist.get("ORANGE"), dist.get("YELLOW"), dist.get("GREEN"),
                        changed, rows.isEmpty() ? 0 : changed * 100.0 / rows.size());
            }

            // ---- 迁移明细：全部关闭时，哪些学生从"有风险"变成"看不出风险"（或反之）
            System.out.println("\n切换方向（全部关闭 vs 基线）最值得关注的迁移：");
            Map<String, Integer> trans = new LinkedHashMap<>();
            for (Row r : rows) {
                String off = levelOf(r, r.factorWith(new BigDecimal[]{ONE, ONE, ONE, ONE}), red, orange, yellow);
                if (!off.equals(r.level)) {
                    trans.merge(r.level + " → " + off, 1, Integer::sum);
                }
            }
            if (trans.isEmpty()) {
                System.out.println("  （无变化）");
            } else {
                trans.entrySet().stream()
                        .sorted((a, b) -> b.getValue() - a.getValue())
                        .forEach(x -> System.out.printf("  %-22s %d 人%n", x.getKey(), x.getValue()));
            }
        }
    }

    // ================================================================ C2 阈值敏感性（单变量扫描）
    private static void sweep(List<Row> rows, BigDecimal red, BigDecimal orange, BigDecimal yellow) {
        System.out.println("单变量阈值扫描（其余阈值固定，单位：人）\n");
        System.out.printf("%-8s %-8s %5s %5s %5s %5s%n", "阈值项", "取值", "红", "橙", "黄", "绿");
        for (String which : new String[]{"red", "orange", "yellow"}) {
            BigDecimal center = "red".equals(which) ? red : "orange".equals(which) ? orange : yellow;
            for (int delta = -5; delta <= 5; delta += (int) 2.5 == 2 ? 2 : 1) {
                // 保持 5 分步长：-5, 0, +5 三档 + 中间两档（±2.5 用 2 与 3 近似）
                if (delta != -5 && delta != -3 && delta != 0 && delta != 3 && delta != 5) {
                    continue;
                }
                BigDecimal v = center.add(BigDecimal.valueOf(delta));
                BigDecimal r = "red".equals(which) ? v : red;
                BigDecimal o = "orange".equals(which) ? v : orange;
                BigDecimal y = "yellow".equals(which) ? v : yellow;
                Map<String, Integer> dist = new LinkedHashMap<>();
                dist.put("RED", 0);
                dist.put("ORANGE", 0);
                dist.put("YELLOW", 0);
                dist.put("GREEN", 0);
                for (Row row : rows) {
                    dist.merge(levelOf(row, row.factorAll(), r, o, y), 1, Integer::sum);
                }
                System.out.printf("%-8s %-8s %5d %5d %5d %5d%n", which, v.toPlainString(),
                        dist.get("RED"), dist.get("ORANGE"), dist.get("YELLOW"), dist.get("GREEN"));
            }
        }
    }
    // ================================================================ 复算核心（对应 RiskEngineService.decide）
    private static String levelOf(Row r, BigDecimal factor, BigDecimal red, BigDecimal orange, BigDecimal yellow) {
        // 1) 分数型风险 × 乘子（只在 base 存在时乘）
        BigDecimal scorePart = BigDecimal.ZERO;
        if (r.base != null) {
            scorePart = RiskMath.clamp100(r.base.multiply(factor).setScale(2, RoundingMode.HALF_UP));
        }
        // 2) 确定性门槛地板不连乘，取最大
        BigDecimal floor = RiskMath.guardFloor(r.hardFail, r.unreachable, r.remaining);
        BigDecimal risk = RiskMath.clamp100(scorePart.max(floor));
        return RiskMath.riskLevel(risk, red, orange, yellow);
    }

    // ================================================================ 数据读取
    private record Row(long enrollmentId, BigDecimal base, boolean hardFail, boolean unreachable,
                       Integer remaining, String level, BigDecimal courseTypeFactor, BigDecimal failRateFactor,
                       BigDecimal teacherFactor, BigDecimal personalFactor) {

        BigDecimal factorAll() {
            return courseTypeFactor.multiply(failRateFactor).multiply(teacherFactor).multiply(personalFactor)
                    .setScale(4, RoundingMode.HALF_UP);
        }

        /** overrides = {courseType, failRate, teacher, personal}，null 表示沿用原值 */
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
        String sql = """
                select enrollment_id, channel_b_score, hard_fail, final_unreachable, remaining_absent,
                       risk_level, data_quality_json
                from risk_state
                where term_id = ? and is_void = 0 and risk_level is not null
                order by enrollment_id
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, termId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String json = rs.getString(7);
                    JsonNode node = json == null ? null : MAPPER.readTree(json);
                    out.add(new Row(
                            rs.getLong(1),
                            rs.getBigDecimal(2),
                            rs.getInt(3) == 1,
                            rs.getInt(4) == 1,
                            rs.getObject(5) == null ? null : rs.getInt(5),
                            rs.getString(6),
                            num(node, "courseTypeFactor"),
                            num(node, "failRateFactor"),
                            num(node, "teacherFactor"),
                            num(node, "personalFactor")));
                }
            }
        }
        return out;
    }

    private static BigDecimal num(JsonNode node, String key) {
        if (node == null) {
            return ONE;
        }
        JsonNode v = node.get(key);
        if (v == null || v.isNull()) {
            return ONE;
        }
        try {
            return new BigDecimal(v.asText());
        } catch (Exception e) {
            return ONE;
        }
    }
}
