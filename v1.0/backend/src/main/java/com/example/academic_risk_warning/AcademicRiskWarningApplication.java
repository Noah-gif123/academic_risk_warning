package com.example.academic_risk_warning;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.academic_risk_warning.mapper")
@EnableScheduling
@EnableAsync
public class   AcademicRiskWarningApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcademicRiskWarningApplication.class, args);
    }

}
