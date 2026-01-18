package com.goodda.jejuday;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@EnableScheduling
@SpringBootApplication
@EntityScan(basePackageClasses={User.class, Spot.class, SpotViewLog.class})
public class JejudayApplication {

    public static void main(String[] args) {
        SpringApplication.run(JejudayApplication.class, args);
    }

}
