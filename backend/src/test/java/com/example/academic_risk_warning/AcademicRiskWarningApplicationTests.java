package com.example.academic_risk_warning;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Boot 集成测试（需要 MySQL 数据库，CI 环境下可能禁用）
 */
@SpringBootTest
@Disabled("需要 MySQL 数据库连接，在缺少外部数据库的 CI 环境下跳过")
class AcademicRiskWarningApplicationTests {

    @Test
    void contextLoads() {
    }

}
