package com.example.jyh1013;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.jyh1013.mapper")
public class Jyh1013Application {

    public static void main(String[] args) {
        SpringApplication.run(Jyh1013Application.class, args);
    }

}
