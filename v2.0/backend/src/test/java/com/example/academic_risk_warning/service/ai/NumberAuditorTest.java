package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.vo.ai.AdminClassFact;
import com.example.academic_risk_warning.vo.ai.ClassFact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 报告数字对账的口径测试：班级总数与课程级风险率不能互相误判。 */
class NumberAuditorTest {

    @Test
    @DisplayName("班级报告的班级总数、红黄人数、平均分一致")
    void classFactsPass() {
        ClassFact f = new ClassFact();
        f.setStudentCount(40);
        f.setRedCount(3);
        f.setYellowCount(20);
        f.setGreenCount(17);
        f.setAvgScore(new BigDecimal("73.1"));
        f.setPassScore(60);

        List<NumberAuditor.Check> checks = NumberAuditor.audit(
                "全班 40 人，红灯 3 人，黄灯 20 人，平均 73.1 分。",
                NumberAuditor.fromClass(f));

        assertFalse(checks.isEmpty());
        assertTrue(checks.stream().allMatch(NumberAuditor.Check::ok),
                () -> "不应有误报：" + checks);
    }

    @Test
    @DisplayName("行政班报告允许单门课程风险率与全班风险率并存")
    void adminCourseRiskRatePasses() {
        AdminClassFact f = new AdminClassFact();
        f.setStudentCount(40);
        f.setRedCount(29);
        f.setYellowCount(79);
        f.setGreenCount(155);
        f.setFailRate(new BigDecimal("10.4"));
        AdminClassFact.CourseRisk c = new AdminClassFact.CourseRisk();
        c.setCourseName("大学计算机基础（一）");
        c.setFailRate(new BigDecimal("7.5"));
        f.setCourseRisks(List.of(c));

        List<NumberAuditor.Check> checks = NumberAuditor.audit(
                "该课程风险率 7.5%，全班风险率 10.4%。",
                NumberAuditor.fromAdminClass(f));

        assertFalse(checks.isEmpty());
        assertTrue(checks.stream().allMatch(NumberAuditor.Check::ok),
                () -> "课程级风险率不应被误判：" + checks);
    }
}
