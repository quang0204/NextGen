package com.wedbackendservice.nextgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class NextGenApplication {

    public static void main(String[] args) {
        SpringApplication.run(NextGenApplication.class, args);
    }

}
