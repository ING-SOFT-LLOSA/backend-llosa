package com.llosa.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendLlosaApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendLlosaApplication.class, args);
    }
}

