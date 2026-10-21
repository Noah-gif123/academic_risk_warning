package com.example.academic_risk_warning;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 学情预警系统 v2.0 启动类
 * <p>四端（学生 / 教师 / 导员 / 教务）统一入口，端口 8080。</p>
 */
@EnableScheduling
@SpringBootApplication
@MapperScan("com.example.academic_risk_warning.mapper")
public class AcademicRiskWarningApplication {
    public static void main(String[] args) {
        SpringApplication.run(AcademicRiskWarningApplication.class, args);
    }
}
