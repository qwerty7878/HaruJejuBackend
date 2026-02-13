package com.goodda.jejuday.crawler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Semaphore;

@Configuration
public class CrawlerConfig {

    /**
     * 크롤러 동시 실행 제어용 Semaphore
     * 최대 1개의 크롤러만 동시 실행 가능
     */
    @Bean
    public Semaphore crawlerSemaphore() {
        return new Semaphore(1);
    }
}