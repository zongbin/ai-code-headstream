package com.leon.aicodeheadstream;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.leon.aicodeheadstream.mapper")
public class AiCodeHeadstreamApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeHeadstreamApplication.class, args);
    }

}
