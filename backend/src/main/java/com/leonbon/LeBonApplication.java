package com.leonbon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class LeBonApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeBonApplication.class, args);
    }
}

