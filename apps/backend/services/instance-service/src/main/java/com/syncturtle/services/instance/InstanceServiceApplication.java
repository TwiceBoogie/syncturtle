package com.syncturtle.services.instance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients
@EnableScheduling
@SpringBootApplication
public class InstanceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InstanceServiceApplication.class, args);
    }
}
