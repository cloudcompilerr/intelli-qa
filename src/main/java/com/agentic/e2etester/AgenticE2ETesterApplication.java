package com.agentic.e2etester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableKafka
@EnableAsync
public class AgenticE2ETesterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgenticE2ETesterApplication.class, args);
    }
}