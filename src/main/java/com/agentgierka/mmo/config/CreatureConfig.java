package com.agentgierka.mmo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class CreatureConfig {

    @Bean
    public Random random() {
        return new Random();
    }
}
