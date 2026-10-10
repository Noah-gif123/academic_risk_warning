package com.example.redchina;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 确保basePackages指向你的Mapper接口所在包
@MapperScan(basePackages = "com.example.redchina.mapper")
@SpringBootApplication
public class RedChinaApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedChinaApplication.class, args);
    }
}
