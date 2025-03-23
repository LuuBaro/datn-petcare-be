package org.example.petcarebe.config;

import org.example.petcarebe.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtServiceConfig {

    @Bean
    public JwtService jwtService() {
        return new JwtService();
    }
}