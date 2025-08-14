package com.goodda.jejuday.crawler.service;

import com.goodda.jejuday.crawler.service.JejuEventCrawlerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class JejuEventCrawlerServiceTest {

    @Autowired
    private JejuEventCrawlerService crawlerService;

    @Test
    public void testCrawlNewEvents_now() throws Exception {
        // 실제 크롤링 로직 즉시 실행
        crawlerService.crawlNewEvents();
        // 이후 repository 상태 검증 로직 추가 가능
    }
}
