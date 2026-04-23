package com.agentgierka.mmo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

@Configuration
@EnableResilientMethods
public class RetryConfig {
}
