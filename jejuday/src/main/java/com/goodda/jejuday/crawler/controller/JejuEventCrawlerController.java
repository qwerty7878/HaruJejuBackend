package com.goodda.jejuday.crawler.controller;

import com.goodda.jejuday.crawler.service.JejuEventCrawlerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/crawler")
public class JejuEventCrawlerController {
    private final JejuEventCrawlerService crawlerService;

    public JejuEventCrawlerController(JejuEventCrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    /**
     * 즉시 크롤링 실행
     */
    @GetMapping("/run")
    public ResponseEntity<String> runCrawlerNow() {
        try {
            crawlerService.crawlNewEvents();
            return ResponseEntity.ok("Crawling initiated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error during crawling: " + e.getMessage());
        }
    }

    @GetMapping("/raw")
    public List<String> getRawLiHtml() throws IOException {
        return crawlerService.fetchRawLiHtml();
    }

}