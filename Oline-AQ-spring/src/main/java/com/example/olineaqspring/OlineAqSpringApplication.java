package com.example.olineaqspring;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.olineaqspring.mapper")
@SpringBootApplication
public class OlineAqSpringApplication {
    public static void main(String[] args) {
        SpringApplication.run(OlineAqSpringApplication.class, args);
    }
}
