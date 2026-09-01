package com.computershare.regfiling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // DeadlineRiskEvaluator
public class RegFilingApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegFilingApplication.class, args);
    }
}
