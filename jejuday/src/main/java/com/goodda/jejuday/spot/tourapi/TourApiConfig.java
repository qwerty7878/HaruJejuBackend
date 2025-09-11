package com.goodda.jejuday.spot.tourapi;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(TourApiProperties.class)
public class TourApiConfig {

    @Bean
    public WebClient tourWebClient(TourApiProperties props) {
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                        .build())
                .build();
    }

    @Bean(name = "downloadWebClient")
    public WebClient downloadWebClient() {
        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(15)); // 10~20초 정도
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(http))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(-1))
                        .build())
                .build();
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager tm) {
        TransactionTemplate t = new TransactionTemplate(tm);
        t.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        t.setTimeout(30); // 아이템당 30초 등 합리적인 값
        return t;
    }
}