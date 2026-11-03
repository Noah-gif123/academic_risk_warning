import java.sql.*;
public class Q {
    private static final java.io.PrintStream OUT;
    static {
        try { OUT = new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true, "UTF-8"); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://127.0.0.1:3306/study_warning_system_v2?useUnicode=true&characterEncoding=utf8"
                + "&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false";
        try (Connection c = DriverManager.getConnection(url, "root", "root")) {
            for (String sql : args) {
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
                                    if (v != null && v.length() > 160) v = v.substring(0, 160) + "...";
                                    line.append(i > 1 ? "\t" : "").append(v == null ? "NULL" : v.replace('\n', ' '));
                                }
                                OUT.println(line);
                            }
                        }
                    } else {
                        OUT.println("UPDATE " + st.getUpdateCount() + " rows");
                    }
                }
            }
        }
    }
}