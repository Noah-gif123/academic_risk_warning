import com.example.academic_risk_warning.service.ai.NumberAuditor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数字对账的独立自测（不需要起服务）：
 *   java -cp "build-verify;<jars>" tool/audit/NumberAuditDemo.java
 *
 * 用一段"故意写错两个数字"的正文，验证对账能把不一致点抓出来。
 */
public class NumberAuditDemo {
    public static void main(String[] args) {
        NumberAuditor.Facts facts = new NumberAuditor.Facts(
                45,          // studentCount
                1,           // red
                15,          // yellow
                29,          // green
                null,        // courseCount
                1,           // classCount
                45,          // enrollmentCount
                new BigDecimal("30.9"),   // avgScore
                new BigDecimal("12.0"),   // minScore
                new BigDecimal("58.0"),   // maxScore
                60,          // passScore
                null,        // failRate
                3,           // absentOver
                16           // activeAlerts
        );
        String text = "本班共 45 人，红灯 1 人、黄灯 15 人，班级平均 30.9 分，最低 12 分，及格线 60 分，"
                + "另有红灯 4 人需要约谈，未关闭预警事件 16 条。";
        List<NumberAuditor.Check> checks = NumberAuditor.audit(text, facts);
        int passed = 0;
        for (NumberAuditor.Check c : checks) {
            if (c.ok()) {
                passed++;
            }
            System.out.println((c.ok() ? "OK   " : "MISS ") + c.label()
                    + "  报告写=" + c.claim() + "  事实=" + c.expected());
        }
        System.out.println("对账 " + passed + "/" + checks.size() + " 一致");

        String text2 = "本班共 45 人，红灯 1 人、黄灯 15 人，班级平均 30.9 分，最低 12 分，及格线 60 分，"
                + "红黄灯合计 16 人，缺勤超上限 3 人，未关闭预警事件 16 条。";
        List<NumberAuditor.Check> checks2 = NumberAuditor.audit(text2, facts);
        int passed2 = checks2.stream().filter(NumberAuditor.Check::ok).count() == checks2.size() ? checks2.size() : 0;
        System.out.println("全文对账（应全一致）：" + passed2 + "/" + checks2.size());
    }
}
