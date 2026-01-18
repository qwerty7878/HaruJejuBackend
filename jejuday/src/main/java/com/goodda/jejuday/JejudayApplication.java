package com.goodda.jejuday;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import com.goodda.jejuday.auth.entity.User;
import com.goodda.jejuday.spot.entity.Spot;
import com.goodda.jejuday.spot.entity.SpotViewLog;

@EnableScheduling
@SpringBootApplication
@EntityScan(basePackageClasses={User.class, Spot.class, SpotViewLog.class})
public class JejudayApplication {

    public static void main(String[] args) {
        SpringApplication.run(JejudayApplication.class, args);
    }

}
