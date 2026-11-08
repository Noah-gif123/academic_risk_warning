import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * P1-0 演示数据生命周期生成器（v3.0）。
 *
 * <p>目标：让 5 个闭环指标与对照组在 v3.0 演示库里有真实可算的数据：
 * <ul>
 *   <li>告警生命周期：ACTIVE → HANDLING → CLOSED，create/handle/close 时间分散到 8 周；</li>
 *   <li>干预记录：TREAT（60%）按 7 天窗口复评；CONTROL（40%）为延迟干预对照组，仅复评；</li>
 *   <li>复评等级：写入 intervention_record.level_before / level_after / cohort（P1-2 新列）；</li>
 *   <li>历史快照：回填 stat_daily_class 最近 8 周（供 P1-3 物化周 KPI / P1-5 趋势图）。</li>
 * </ul>
 * 确定性：同一 seed 同一结果；执行前 TRUNCATE 生命周期派生表，可重复运行。
 * 只读 v2.0 演示库不受影响（只连传入的 v3 库）。
 *
 * <p>用法：{@code java -cp "gen_classes;mysql-connector-j-9.2.0.jar" GenLifecycle
 * --db=study_warning_system_v3 --term=5 --seed=20261108}</p>
 */
public class GenLifecycle {

    private static final String[] LEVELS = {"GREEN", "YELLOW", "ORANGE", "RED"};

    public static void main(String[] args) throws Exception {
        String db = "study_warning_system_v3";
        long termId = 5L;
        long seed = 20261108L;
        for (String a : args) {
            if (a.startsWith("--db=")) db = a.substring(5);
            else if (a.startsWith("--term=")) termId = Long.parseLong(a.substring(7));
            else if (a.startsWith("--seed=")) seed = Long.parseLong(a.substring(7));
        }
        String url = "jdbc:mysql://127.0.0.1:3306/" + db
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
                + "&allowPublicKeyRetrieval=true&useSSL=false";
        try (Connection conn = DriverManager.getConnection(url, "root", "root")) {
            conn.setAutoCommit(false);
            try {
                run(conn, db, termId, seed);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static void run(Connection conn, String db, long termId, long seed) throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(0).minusHours(2);
        LocalDate termStart = queryDate(conn, "select start_date from term where id = ?", termId);
        String engineVersion = queryString(conn, "select max(engine_version) from risk_state where term_id = ?", termId);

        System.out.println("P1-0 GenLifecycle: db=" + db + " term=" + termId + " seed=" + seed
                + " termStart=" + termStart + " now=" + now + " engine=" + engineVersion);

        // 0) 清空生命周期派生数据（可重复执行）
        try (Statement st = conn.createStatement()) {
            st.execute("DELETE FROM alert_flow_log");
            st.execute("DELETE FROM intervention_record");
            st.execute("DELETE FROM stat_daily_class");
        }

        // 1) 告警生命周期
        Map<Long, Integer> counselorOfClass = counselorByClass(conn, termId);
        Map<Long, Long> teacherOfClass = teacherByClass(conn);
        Map<Long, String> counselorNameOf = counselorNames(conn);
        List<Alert> alerts = loadAlerts(conn, termId);
        int created = 0, handled = 0, closed = 0;
        Map<Long, String> statusById = new HashMap<>();
        try (PreparedStatement upd = conn.prepareStatement(
                "update alert_record set status=?, handle_by=?, handle_role=?, handle_time=?, handle_note=?,"
                        + " close_by=?, close_time=?, close_note=?, notified=1, first_notify_time=?,"
                        + " create_time=?, update_time=? where id=?");
             PreparedStatement flow = conn.prepareStatement(
                     "insert into alert_flow_log (alert_id, student_id, term_id, event_type, from_level, to_level,"
                             + " from_status, to_status, detail, operator_id, operator_role, event_time, create_time)"
                             + " values (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            for (Alert a : alerts) {
                Random r = new Random(seed * 1000003L + a.id);
                LocalDate createDay = termStart.plusDays(14 + r.nextInt(52));
                LocalDateTime createTime = createDay.atTime(8 + r.nextInt(10), r.nextInt(60), r.nextInt(60));
                if (createTime.isAfter(now.minusDays(1))) {
                    createTime = now.minusDays(1).minusHours(r.nextInt(6));
                }
                double p = r.nextDouble();
                String status;
                LocalDateTime handleTime = null, closeTime = null;
                int handleBy = 0;
                String handleRole = null;
                if (p < 0.55) {
                    status = "CLOSED";
                    handleTime = min(createTime.plusHours(2 + r.nextInt(70)), now.minusHours(6));
                    closeTime = min(handleTime.plusDays(1 + r.nextInt(12)), now.minusHours(1));
                    if (closeTime.isBefore(handleTime)) {
                        closeTime = handleTime;
                    }
                } else if (p < 0.80) {
                    status = "HANDLING";
                    handleTime = min(createTime.plusHours(2 + r.nextInt(70)), now.minusHours(6));
                    if (handleTime.isBefore(createTime)) {
                        handleTime = createTime.plusHours(4);
                    }
                } else {
                    status = "ACTIVE";
                }
                statusById.put(a.id, status);
                if (!"ACTIVE".equals(status)) {
                    Integer cid = counselorOfClass.get(a.teachingClassId);
                    handleBy = cid == null ? 1 : cid;
                    handleRole = "COUNSELOR";
                }
                int closeBy = 0;
                String closeRole = null;
                if ("CLOSED".equals(status)) {
                    closeBy = handleBy;
                    closeRole = "COUNSELOR";
                }
                upd.setString(1, status);
                upd.setObject(2, "ACTIVE".equals(status) ? null : handleBy);
                upd.setObject(3, handleRole);
                upd.setObject(4, handleTime == null ? null : Timestamp.valueOf(handleTime));
                upd.setObject(5, "ACTIVE".equals(status) ? null : "已联系学生并记录处置（演示数据）");
                upd.setObject(6, "CLOSED".equals(status) ? closeBy : null);
                upd.setObject(7, closeTime == null ? null : Timestamp.valueOf(closeTime));
                upd.setObject(8, "CLOSED".equals(status) ? "风险降级或已处置完成，关闭预警（演示）" : null);
                upd.setTimestamp(9, Timestamp.valueOf(createTime.plusMinutes(5)));
                upd.setTimestamp(10, Timestamp.valueOf(createTime));
                upd.setTimestamp(11, Timestamp.valueOf(closeTime != null ? closeTime
                        : handleTime != null ? handleTime : createTime));
                upd.setLong(12, a.id);
                upd.addBatch();
                created++;
                if (handleTime != null) {
                    handled++;
                }
                if (closeTime != null) {
                    closed++;
                }

                // 时间线
                addFlow(flow, termId, a, "CREATED", null, a.level, null, "ACTIVE",
                        "生成预警（P1-0 演示时间线）", null, "SYSTEM", createTime);
                addFlow(flow, termId, a, "NOTIFIED", a.level, a.level, "ACTIVE", "ACTIVE",
                        "已通知任课教师、辅导员与学生", null, "SYSTEM", createTime.plusMinutes(5));
                if (handleTime != null) {
                    addFlow(flow, termId, a, "ACKNOWLEDGED", a.level, a.level, "ACTIVE", "HANDLING",
                            "辅导员认领并开始处置", handleBy, "COUNSELOR", handleTime);
                    addFlow(flow, termId, a, "HANDLED", a.level, a.level, "HANDLING", "HANDLING",
                            "已约谈/联系学生并登记处置", handleBy, "COUNSELOR", handleTime.plusMinutes(30));
                }
                if (closeTime != null) {
                    addFlow(flow, termId, a, "CLOSED", a.level, a.level,
                            handleTime != null ? "HANDLING" : "ACTIVE", "CLOSED",
                            "处置完成或风险降级，关闭预警", closeBy, "COUNSELOR", closeTime);
                }
            }
            upd.executeBatch();
            flow.executeBatch();
        }

        // 2) 干预 + 对照组复评
        int treat = 0, control = 0;
        try (PreparedStatement ins = conn.prepareStatement(
                "insert into intervention_record (student_id, term_id, alert_id, related_alert_ids, stage,"
                        + " operator_id, operator_role, action_type, content, result, effective, effect_check_time,"
                        + " risk_score_before, risk_score_after, risk_score_change, level_before, level_after, cohort,"
                        + " refer_status, is_void, create_by, create_time, update_by, update_time)"
                        + " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,?,?,?,?)")) {
            for (Alert a : alerts) {
                if ("GREEN".equals(a.level) || "ACTIVE".equals(statusById.get(a.id))) {
                    continue;
                }
                Random r = new Random(seed * 2000003L + a.id);
                boolean treatCohort = isTreat(a.studentId, seed);
                long h = Math.floorMod((long) a.studentId * 7919L + (treatCohort ? 104729L : 130363L) + a.id * 31L, 100L);
                String outcome;
                if (treatCohort) {
                    outcome = h < 58 ? "IMPROVED" : (h < 86 ? "UNCHANGED" : "WORSENED");
                } else {
                    outcome = h < 27 ? "IMPROVED" : (h < 85 ? "UNCHANGED" : "WORSENED");
                }
                String before = a.level;
                String after;
                if ("IMPROVED".equals(outcome)) {
                    after = downLevel(before, r.nextDouble() < 0.25 ? 2 : 1);
                } else if ("WORSENED".equals(outcome)) {
                    after = upLevel(before);
                } else {
                    after = before;
                }
                java.math.BigDecimal beforeScore = a.riskScore == null
                        ? java.math.BigDecimal.valueOf(60) : a.riskScore;
                java.math.BigDecimal afterScore;
                if ("IMPROVED".equals(outcome)) {
                    afterScore = beforeScore.subtract(java.math.BigDecimal.valueOf(8 + r.nextInt(12)));
                } else if ("WORSENED".equals(outcome)) {
                    afterScore = beforeScore.add(java.math.BigDecimal.valueOf(5 + r.nextInt(10)));
                } else {
                    afterScore = beforeScore.add(java.math.BigDecimal.valueOf(r.nextInt(3) - 1));
                }
                if (afterScore.signum() < 0) afterScore = java.math.BigDecimal.ZERO;
                if (afterScore.compareTo(java.math.BigDecimal.valueOf(100)) > 0) afterScore = java.math.BigDecimal.valueOf(100);
                afterScore = afterScore.setScale(1, java.math.RoundingMode.HALF_UP);
                java.math.BigDecimal change = afterScore.subtract(beforeScore).setScale(1, java.math.RoundingMode.HALF_UP);
                String effective = "IMPROVED".equals(outcome) ? "EFFECTIVE" : "INEFFECTIVE";
                LocalDate createDay = termStart.plusDays(21 + r.nextInt(45));
                LocalDateTime createTime = createDay.atTime(9 + r.nextInt(9), r.nextInt(60), 0);
                if (createTime.isAfter(now.minusDays(8))) {
                    createTime = now.minusDays(8).minusHours(r.nextInt(6));
                }
                LocalDateTime checkTime = createTime.plusDays(7 + r.nextInt(3));
                if (checkTime.isAfter(now)) {
                    checkTime = null;
                    effective = "PENDING";
                }
                Integer cid = counselorOfClass.get(a.teachingClassId);
                int operator = cid == null ? 1 : cid;
                String stage = treatCohort ? (r.nextDouble() < 0.5 ? "FIRST" : "COUNSELOR") : "COUNSELOR";
                String action = treatCohort
                        ? new String[]{"TALK", "PARENT", "MEETING", "OTHER"}[r.nextInt(4)] : "DELAYED";
                String content = treatCohort
                        ? "P1-0 演示干预：约谈学生并制定 7 天改进计划"
                        : "对照组：本周期不干预，仅做 T+7 随访复评（延迟干预组）";
                String result = treatCohort ? "已登记处置结果（演示）" : "仅复评，未实施干预（对照组）";
                ins.setLong(1, a.studentId);
                ins.setLong(2, termId);
                ins.setObject(3, a.id);
                ins.setString(4, "[" + a.id + "]");
                ins.setString(5, stage);
                ins.setLong(6, operator);
                ins.setString(7, "COUNSELOR");
                ins.setString(8, action);
                ins.setString(9, content);
                ins.setString(10, result);
                ins.setString(11, effective);
                ins.setObject(12, checkTime == null ? null : Timestamp.valueOf(checkTime));
                ins.setBigDecimal(13, beforeScore);
                ins.setBigDecimal(14, afterScore);
                ins.setBigDecimal(15, change);
                ins.setString(16, before);
                ins.setString(17, checkTime == null ? null : after);
                ins.setString(18, treatCohort ? "TREAT" : "CONTROL");
                ins.setString(19, null);
                ins.setLong(20, operator);
                ins.setTimestamp(21, Timestamp.valueOf(createTime));
                ins.setObject(22, null);
                ins.setTimestamp(23, Timestamp.valueOf(checkTime == null ? createTime : checkTime));
                ins.addBatch();
                if (treatCohort) treat++; else control++;
            }
            ins.executeBatch();
        }

        // 3) stat_daily_class 历史快照回填（最近 8 周，含本周；供趋势/物化）
        int snapshots = backfillDaily(conn, termId, termStart, now, seed, engineVersion, counselorOfClass);
        System.out.println("done: alerts=" + alerts.size() + " handled=" + handled + " closed=" + closed
                + " interventions TREAT=" + treat + " CONTROL=" + control + " stat_daily_class=" + snapshots);
    }

    // ============================================================ 回填日快照
    private static int backfillDaily(Connection conn, long termId, LocalDate termStart, LocalDateTime now,
                                     long seed, String engineVersion, Map<Long, Integer> counselorOfClass) throws Exception {
        LocalDate currentMonday = LocalDate.now().with(DayOfWeek.MONDAY);
        Map<Long, String> classes = new HashMap<>();
        Map<Long, Map<String, Integer>> current = new HashMap<>();
        Map<Long, Integer> studentCount = new HashMap<>();
        Map<Long, Double> avgRisk = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "select rs.teaching_class_id, tc.class_code, count(*) total,"
                        + " sum(rs.risk_level='RED'), sum(rs.risk_level='ORANGE'),"
                        + " sum(rs.risk_level='YELLOW'), sum(rs.risk_level='GREEN'), avg(rs.risk_score)"
                        + " from risk_state rs join teaching_class tc on tc.id=rs.teaching_class_id"
                        + " where rs.term_id=? and rs.is_void=0 group by rs.teaching_class_id, tc.class_code")) {
            ps.setLong(1, termId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long cid = rs.getLong(1);
                    classes.put(cid, rs.getString(2));
                    studentCount.put(cid, rs.getInt(3));
                    Map<String, Integer> m = new HashMap<>();
                    m.put("RED", rs.getInt(4));
                    m.put("ORANGE", rs.getInt(5));
                    m.put("YELLOW", rs.getInt(6));
                    m.put("GREEN", rs.getInt(7));
                    current.put(cid, m);
                    avgRisk.put(cid, rs.getDouble(8));
                }
            }
        }
        int rows = 0;
        try (PreparedStatement ins = conn.prepareStatement(
                "insert into stat_daily_class (stat_date, teaching_class_id, student_count, red, orange, yellow, green,"
                        + " avg_score, alert_new, alert_closed, intervention_count, engine_version,"
                        + " source_max_update_time, is_stale, create_time, update_time)"
                        + " values (?,?,?,?,?,?,?,?,?,?,?,?,?,0,now(),now())")) {
            for (int w = 0; w <= 8; w++) {
                LocalDate weekMonday = currentMonday.minusWeeks(8 - w);
                LocalDate statDate = weekMonday.plusDays(4); // 周五
                if (statDate.isAfter(LocalDate.now())) {
                    statDate = LocalDate.now();
                }
                for (Map.Entry<Long, String> e : classes.entrySet()) {
                    long cid = e.getKey();
                    Random r = new Random(seed * 3000017L + cid * 131 + w);
                    Map<String, Integer> cur = current.get(cid);
                    int total = studentCount.get(cid);
                    int weeksAgo = 8 - w;
                    // 越早的周红/黄越多，逐渐收敛到当前值（演示回填，非真实历史）
                    int red = Math.min(total, cur.get("RED") + weeksAgo * (1 + (int) (cid % 2)) + (weeksAgo > 3 ? 1 : 0));
                    int orange = Math.min(total - red, cur.get("ORANGE") + weeksAgo * ((int) (cid % 3) == 0 ? 1 : 0));
                    int yellow = Math.min(total - red - orange, cur.get("YELLOW") + weeksAgo * ((int) (cid % 2) == 0 ? 1 : 0));
                    int green = Math.max(0, total - red - orange - yellow);
                    double baseAvg = avgRisk.getOrDefault(cid, 30.0);
                    // 事件计数（按周窗口）
                    int[] ev = eventsForWeek(conn, termId, cid, weekMonday, weekMonday.plusDays(6));
                    ins.setDate(1, java.sql.Date.valueOf(statDate));
                    ins.setLong(2, cid);
                    ins.setInt(3, total);
                    ins.setInt(4, red);
                    ins.setInt(5, orange);
                    ins.setInt(6, yellow);
                    ins.setInt(7, green);
                    ins.setBigDecimal(8, java.math.BigDecimal.valueOf(Math.max(0, baseAvg - weeksAgo * 0.8))
                            .setScale(2, java.math.RoundingMode.HALF_UP));
                    ins.setInt(9, ev[0]);
                    ins.setInt(10, ev[1]);
                    ins.setInt(11, ev[2]);
                    ins.setString(12, engineVersion);
                    ins.setTimestamp(13, Timestamp.valueOf(now));
                    ins.addBatch();
                    rows++;
                }
            }
            ins.executeBatch();
        }
        return rows;
    }

    private static int[] eventsForWeek(Connection conn, long termId, long classId,
                                       LocalDate from, LocalDate to) throws Exception {
        Timestamp s = Timestamp.valueOf(from.atStartOfDay());
        Timestamp e = Timestamp.valueOf(to.plusDays(1).atStartOfDay());
        int created = count(conn, "select count(*) from alert_record where term_id=? and teaching_class_id=?"
                + " and is_void=0 and create_time>=? and create_time<?", termId, classId, s, e);
        int closed = count(conn, "select count(*) from alert_record where term_id=? and teaching_class_id=?"
                + " and is_void=0 and close_time>=? and close_time<?", termId, classId, s, e);
        int intervened = count(conn, "select count(*) from intervention_record ir"
                + " join alert_record ar on ar.id = ir.alert_id"
                + " where ar.term_id=? and ar.teaching_class_id=? and ir.is_void=0"
                + " and ir.create_time>=? and ir.create_time<?",
                termId, classId, s, e);
        return new int[]{created, closed, intervened};
    }

    private static int count(Connection conn, String sql, Object... args) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ============================================================ 数据加载与工具
    private record Alert(long id, long studentId, long teachingClassId, String level,
                         java.math.BigDecimal riskScore, String status) {
    }

    private static List<Alert> loadAlerts(Connection conn, long termId) throws Exception {
        List<Alert> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "select id, student_id, teaching_class_id, alert_level, risk_score, status from alert_record"
                        + " where term_id=? and is_void=0 order by id")) {
            ps.setLong(1, termId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Alert(rs.getLong(1), rs.getLong(2), rs.getLong(3),
                            rs.getString(4), rs.getBigDecimal(5), rs.getString(6)));
                }
            }
        }
        return out;
    }

    private static Map<Long, Integer> counselorByClass(Connection conn, long termId) throws Exception {
        Map<Long, Integer> out = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "select admin_class_id, counselor_id from counselor_class where term_id=?")) {
            ps.setLong(1, termId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getLong(1), rs.getInt(2));
                }
            }
        }
        return out;
    }

    private static Map<Long, Long> teacherByClass(Connection conn) throws Exception {
        Map<Long, Long> out = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("select id, teacher_id from teaching_class");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.put(rs.getLong(1), rs.getLong(2));
            }
        }
        return out;
    }

    private static Map<Long, String> counselorNames(Connection conn) throws Exception {
        Map<Long, String> out = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("select id, counselor_name from counselor");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.put(rs.getLong(1), rs.getString(2));
            }
        }
        return out;
    }

    private static void addFlow(PreparedStatement ps, long termId, Alert a, String type, String fromLevel, String toLevel,
                                String fromStatus, String toStatus, String detail, Integer operator,
                                String role, LocalDateTime time) throws Exception {
        ps.setLong(1, a.id);
        ps.setLong(2, a.studentId);
        ps.setLong(3, termId);
        ps.setString(4, type);
        ps.setString(5, fromLevel);
        ps.setString(6, toLevel);
        ps.setString(7, fromStatus);
        ps.setString(8, toStatus);
        ps.setString(9, detail);
        ps.setObject(10, operator);
        ps.setString(11, role);
        ps.setTimestamp(12, Timestamp.valueOf(time));
        ps.setTimestamp(13, Timestamp.valueOf(time));
        ps.addBatch();
    }

    private static boolean isTreat(long studentId, long seed) {
        long h = (studentId * 2654435761L + seed) & 0xFFFFFFFFL;
        return h < (long) (0.6 * 0x100000000L);
    }

    private static String downLevel(String level, int steps) {
        int idx = indexOf(level);
        int to = Math.max(0, idx - steps);
        return LEVELS[to];
    }

    private static String upLevel(String level) {
        int idx = indexOf(level);
        return LEVELS[Math.min(LEVELS.length - 1, idx + 1)];
    }

    private static int indexOf(String level) {
        for (int i = 0; i < LEVELS.length; i++) {
            if (LEVELS[i].equals(level)) {
                return i;
            }
        }
        return 1;
    }

    private static LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }

    private static LocalDate queryDate(Connection conn, String sql, Object... args) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Date d = rs.getDate(1);
                    return d == null ? LocalDate.now() : d.toLocalDate();
                }
            }
        }
        return LocalDate.now();
    }

    private static String queryString(Connection conn, String sql, Object... args) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
