package com.unibook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableRetry
public class UnibookApplication {

    public static void main(String[] args) {
        // JVM의 기본 시간대를 한국 표준시로 설정 (main 메서드에서 실행)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        
        SpringApplication.run(UnibookApplication.class, args);
    }

}
