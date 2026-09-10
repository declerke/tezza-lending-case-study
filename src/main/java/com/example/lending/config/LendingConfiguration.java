package com.example.lending.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class LendingConfiguration {
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
