import java.sql.*;

/**
 * 涓存椂鏌ュ簱灏忓伐鍏凤紙婕旂ず椤圭洰娌℃湁 mysql 瀹㈡埛绔椂鐢級锛? *   java -cp build-verify-lib/mysql-connector-j-9.2.0.jar Q.java "select ..."
 * 杈撳嚭鍒惰〃绗﹀垎闅旓紝UTF-8銆? */
public class Q {
    private static final java.io.PrintStream OUT;

    static {
        try {
            OUT = new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true, "UTF-8");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://127.0.0.1:3306/study_warning_system_v2?useUnicode=true&characterEncoding=utf8"
                + "&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false";
        try (Connection c = DriverManager.getConnection(url, "root", "root")) {
            for (String sql : args) {
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    ResultSetMetaData md = rs.getMetaData();
                    StringBuilder head = new StringBuilder();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        head.append(i > 1 ? "\t" : "").append(md.getColumnLabel(i));
                    }
                    OUT.println(head);
                    while (rs.next()) {
                        StringBuilder line = new StringBuilder();
                        for (int i = 1; i <= md.getColumnCount(); i++) {
                            String v = rs.getString(i);
                            if (v != null && v.length() > 160) {
                                v = v.substring(0, 160) + "...";
                            }
                            line.append(i > 1 ? "\t" : "").append(v == null ? "NULL" : v.replace('\n', ' '));
                        }
                        OUT.println(line);
                    }
                }
            }
        }
    }
}
