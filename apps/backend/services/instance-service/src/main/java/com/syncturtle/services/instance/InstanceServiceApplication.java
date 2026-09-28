package com.syncturtle.services.instance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.syncturtle.common.security.configuration.ServletAuthCookieConfiguration;

@Import(ServletAuthCookieConfiguration.class)
@SpringBootApplication
public class InstanceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InstanceServiceApplication.class, args);
    }
}
