package com.citizenregistry.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = "com.citizenregistry")
@EntityScan(basePackages = "com.citizenregistry.domain")
public class CitizenServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CitizenServiceApplication.class, args);
    }
}
