package com.agentgierka.mmo.config;

import com.agentgierka.mmo.agent.model.AgentWorldState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configures Redis for storing real-time agent state.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, AgentWorldState> agentWorldStateTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, AgentWorldState> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use Strings for keys
        template.setKeySerializer(new StringRedisSerializer());

        // Use JSON for values (AgentWorldState)
        template.setValueSerializer(new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer());

        return template;
    }

    @Bean
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
