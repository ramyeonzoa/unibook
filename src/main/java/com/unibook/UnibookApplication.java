package com.unibook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class UnibookApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnibookApplication.class, args);
    }

}
