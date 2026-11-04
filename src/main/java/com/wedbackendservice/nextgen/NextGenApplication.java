package com.wedbackendservice.nextgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableAsync
@SpringBootApplication
@EnableJpaAuditing
@EnableMethodSecurity
public class NextGenApplication {

    public static void main(String[] args) {
        SpringApplication.run(NextGenApplication.class, args);
    }

}
