package com.jcode.authidentityservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AuthIdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthIdentityServiceApplication.class, args);
    }

}
