package com.nmamit.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CorporatePaymentsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CorporatePaymentsApplication.class, args);
    }
}
