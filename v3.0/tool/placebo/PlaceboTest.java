import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * P1 数据真实性：对照组差值的置换检验（placebo / permutation test）。
 *
 * <p>做法：把 TREAT/CONTROL 标签随机打乱 N 次，重算“干预组−对照组改善率”差值，
 * 得到零分布；真实差值在零分布中的分位即经验 p 值。p 越小说明差值越不可能由
 * 演示数据的标签分配偶然产生（注意：仍不能替代真实因果识别）。</p>
 *
 * <p>输出：控制台 + v3.0/eval/placebo.json；确定性（固定 seed）。</p>
 */
public class PlaceboTest {

    public static void main(String[] args) throws Exception {
        String db = "study_warning_system_v3";
        long termId = 5L;
        long seed = 20261108L;
        int permutations = 5000;
        String out = "../eval/placebo.json";
        for (String a : args) {
            if (a.startsWith("--db=")) db = a.substring(5);
            else if (a.startsWith("--term=")) termId = Long.parseLong(a.substring(7));
            else if (a.startsWith("--seed=")) seed = Long.parseLong(a.substring(7));
            else if (a.startsWith("--perm=")) permutations = Integer.parseInt(a.substring(7));
            else if (a.startsWith("--out=")) out = a.substring(6);
        }
        String url = "jdbc:mysql://127.0.0.1:3306/" + db
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
                + "&allowPublicKeyRetrieval=true&useSSL=false";
        List<int[]> rows = new ArrayList<>(); // [cohortIndex, improved]
        try (Connection conn = DriverManager.getConnection(url, "root", "root");
             PreparedStatement ps = conn.prepareStatement(
                     "select ir.cohort,"
                             + " (ir.level_after is not null and ir.effect_check_time between ir.create_time + interval 7 day"
                             + "  and ir.create_time + interval 14 day) due,"
                             + " (ir.level_after is not null and ir.effect_check_time between ir.create_time + interval 7 day"
                             + "  and ir.create_time + interval 14 day and"
                             + "  (case ir.level_after when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)"
                             + "  < (case ir.level_before when 'RED' then 4 when 'ORANGE' then 3 when 'YELLOW' then 2 else 1 end)) improved"
                             + " from intervention_record ir where ir.term_id=? and ir.is_void=0"
                             + " and ir.cohort in ('TREAT','CONTROL')")) {
            ps.setLong(1, termId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int due = rs.getInt(2);
                    if (due == 1) {
                        rows.add(new int[]{("TREAT".equals(rs.getString(1)) ? 1 : 0), rs.getInt(3)});
                    }
                }
            }
        }
        if (rows.isEmpty()) {
            System.out.println("没有有效复评记录，无法做置换检验");
            return;
        }
        int treatDue = 0, treatImp = 0, controlDue = 0, controlImp = 0;
        for (int[] r : rows) {
            if (r[0] == 1) { treatDue++; treatImp += r[1]; } else { controlDue++; controlImp += r[1]; }
        }
        double treatRate = pct(treatImp, treatDue);
        double controlRate = pct(controlImp, controlDue);
        double observed = round(treatRate - controlRate);
        double[] nullDiff = new double[permutations];
        List<Integer> labels = new ArrayList<>();
        for (int[] r : rows) labels.add(r[0]);
        long[] improvements = new long[rows.size()];
        for (int i = 0; i < rows.size(); i++) improvements[i] = rows.get(i)[1];
        Random random = new Random(seed);
        int extreme = 0;
        for (int p = 0; p < permutations; p++) {
            Collections.shuffle(labels, random);
            int td = 0, ti = 0, cd = 0, ci = 0;
            for (int i = 0; i < labels.size(); i++) {
                int label = labels.get(i);
                if (label == 1) { td++; ti += improvements[i]; } else { cd++; ci += improvements[i]; }
            }
            double diff = pct(ti, td) - pct(ci, cd);
            nullDiff[p] = diff;
            if (Math.abs(diff) >= Math.abs(observed)) extreme++;
        }
        double pValue = (extreme + 1.0) / (permutations + 1.0);
        java.util.Arrays.sort(nullDiff);
        double q025 = nullDiff[(int) Math.floor(0.025 * permutations)];
        double q975 = nullDiff[(int) Math.min(permutations - 1, Math.ceil(0.975 * permutations) - 1)];
        String json = "{\n"
                + "  \"method\": \"permutation test on TREAT/CONTROL labels\",\n"
                + "  \"db\": \"" + db + "\", \"termId\": " + termId + ", \"seed\": " + seed + ",\n"
                + "  \"permutations\": " + permutations + ",\n"
                + "  \"treat\": {\"due\": " + treatDue + ", \"improved\": " + treatImp + ", \"rate\": " + treatRate + "},\n"
                + "  \"control\": {\"due\": " + controlDue + ", \"improved\": " + controlImp + ", \"rate\": " + controlRate + "},\n"
                + "  \"observedDiffPp\": " + observed + ",\n"
                + "  \"nullQuantiles\": {\"q025\": " + round(q025) + ", \"q975\": " + round(q975) + "},\n"
                + "  \"pValue\": " + round4(pValue) + ",\n"
                + "  \"significantAt05\": " + (pValue < 0.05) + ",\n"
                + "  \"note\": \"P1-0 为构造数据；置换检验仅说明差值非标签随机所致，不能替代真实因果识别\"\n"
                + "}";
        Path path = Path.of(out);
        Files.createDirectories(path.getParent());
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
            w.println(json);
        }
        System.out.println(json);
        System.out.println("saved -> " + path.toAbsolutePath());
    }

    private static double pct(int n, int d) {
        return d == 0 ? 0 : n * 100.0 / d;
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
