package com.protectalk.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.protectalk.resource"})
public class ProtecTalkApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProtecTalkApplication.class, args);
    }
}
