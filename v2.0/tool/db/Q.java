import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class Q {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://127.0.0.1:3306/study_warning_system_v2?useUnicode=true&characterEncoding=utf8"
                + "&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false";
        String outFile = null;
        List<String> sqls = new ArrayList<>();
        for (String a : args) {
            if (a.startsWith("@out=")) { outFile = a.substring(5); continue; }
            if (a.startsWith("@")) {
                String text = new String(Files.readAllBytes(Paths.get(a.substring(1))), java.nio.charset.StandardCharsets.UTF_8);
                StringBuilder merged = new StringBuilder();
                for (String line : text.split("\\R")) {
                    String t = line.trim();
                    if (t.startsWith("--") || t.isEmpty()) continue;
                    merged.append(line).append('\n');
                }
                for (String s : merged.toString().split(";")) {
                    if (!s.trim().isEmpty()) sqls.add(s.trim());
                }
            } else {
                sqls.add(a);
            }
        }
        PrintStream OUT = outFile == null
                ? new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8")
                : new PrintStream(new FileOutputStream(outFile), true, "UTF-8");
        try (Connection c = DriverManager.getConnection(url, "root", "root")) {
            for (String sql : sqls) {
                try (Statement st = c.createStatement()) {
                    boolean hasRs = st.execute(sql);
                    if (hasRs) {
                        try (ResultSet rs = st.getResultSet()) {
                            ResultSetMetaData md = rs.getMetaData();
                            StringBuilder head = new StringBuilder();
                            for (int i = 1; i <= md.getColumnCount(); i++) head.append(i > 1 ? "\t" : "").append(md.getColumnLabel(i));
                            OUT.println(head);
                            while (rs.next()) {
                                StringBuilder line = new StringBuilder();
                                for (int i = 1; i <= md.getColumnCount(); i++) {
                                    String v = rs.getString(i);
                                    if (v != null && v.length() > 4000) v = v.substring(0, 4000) + "...";
                                    line.append(i > 1 ? "\t" : "").append(v == null ? "NULL" : v.replace('\n', ' ').replace('\r', ' '));
                                }
                                OUT.println(line);
                            }
                        }
                    } else {
                        OUT.println("UPDATE " + st.getUpdateCount() + " rows");
                    }
                } catch (SQLException e) {
                    OUT.println("FAILED: " + e.getMessage());
                }
            }
        }
        OUT.flush();
        if (outFile != null) OUT.close();
    }
}