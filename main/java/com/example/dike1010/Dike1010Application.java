package com.example.dike1010;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // 自动扫描当前包及其子包下的 Controller、Service 等组件
@MapperScan("com.example.dike1010.mapper") // 扫描 Mapper 接口
public class Dike1010Application {

    public static void main(String[] args) {
        SpringApplication.run(Dike1010Application.class, args);
    }

}
