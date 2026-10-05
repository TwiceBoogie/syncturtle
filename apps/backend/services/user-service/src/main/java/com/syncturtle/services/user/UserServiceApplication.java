package com.syncturtle.services.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.syncturtle.common.security.configuration.ServletAuthCookieConfiguration;

@Import(ServletAuthCookieConfiguration.class)
@SpringBootApplication
public class UserServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(UserServiceApplication.class, args);
  }
}
