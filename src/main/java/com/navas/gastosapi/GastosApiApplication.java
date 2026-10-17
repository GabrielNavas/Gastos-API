package com.navas.gastosapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class GastosApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GastosApiApplication.class, args);
    }
}
