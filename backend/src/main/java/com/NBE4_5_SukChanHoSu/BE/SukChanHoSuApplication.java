package com.NBE4_5_SukChanHoSu.BE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SukChanHoSuApplication {

    public static void main(String[] args) {
        SpringApplication.run(SukChanHoSuApplication.class, args);
    }
}
