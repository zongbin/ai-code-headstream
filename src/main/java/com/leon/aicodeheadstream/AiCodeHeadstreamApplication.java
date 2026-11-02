package com.leon.aicodeheadstream;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.leon.aicodeheadstream.mapper")
public class AiCodeHeadstreamApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeHeadstreamApplication.class, args);
    }

}
